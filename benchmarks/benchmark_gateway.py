#!/usr/bin/env python3
"""Đo độ trễ tuần tự khi gọi cùng API trực tiếp và qua Nginx Gateway."""

import argparse
import json
import math
import os
import platform
import statistics
import sys
import time
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def percentile(values: list[float], percentile_value: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = max(0, math.ceil(percentile_value * len(ordered)) - 1)
    return ordered[index]


def call_api(url: str, token: str | None, timeout: float) -> tuple[float, int | None, str | None]:
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = Request(url, headers=headers, method="GET")
    started = time.perf_counter()
    try:
        with urlopen(request, timeout=timeout) as response:
            response.read()
            return (time.perf_counter() - started) * 1000, response.status, None
    except HTTPError as error:
        error.read()
        return (time.perf_counter() - started) * 1000, error.code, None
    except (URLError, TimeoutError, OSError) as error:
        return (time.perf_counter() - started) * 1000, None, type(error).__name__


def run_series(
    url: str,
    token: str | None,
    warmup: int,
    request_count: int,
    requests_per_second: float,
    timeout: float,
) -> dict:
    interval = 1.0 / requests_per_second
    next_start = time.perf_counter()
    latencies: list[float] = []
    statuses: dict[str, int] = {}
    transport_errors: dict[str, int] = {}
    successful = 0
    completed_started: float | None = None

    for index in range(warmup + request_count):
        wait = next_start - time.perf_counter()
        if wait > 0:
            time.sleep(wait)
        if index == warmup:
            completed_started = time.perf_counter()
        latency_ms, status, error = call_api(url, token, timeout)
        next_start = time.perf_counter() + interval

        if index < warmup:
            continue
        if status is not None:
            latencies.append(latency_ms)
            status_key = str(status)
            statuses[status_key] = statuses.get(status_key, 0) + 1
            if 200 <= status < 300:
                successful += 1
        if error is not None:
            transport_errors[error] = transport_errors.get(error, 0) + 1

    elapsed_seconds = time.perf_counter() - completed_started if completed_started else 0.0
    return {
        "url": url,
        "requests": request_count,
        "successful": successful,
        "http_errors": request_count - successful - sum(transport_errors.values()),
        "transport_errors": transport_errors,
        "status_codes": statuses,
        "elapsed_seconds": round(elapsed_seconds, 3),
        "successful_requests_per_second": round(successful / elapsed_seconds, 3)
        if elapsed_seconds > 0
        else 0,
        "latency_ms": {
            "min": round(min(latencies), 3) if latencies else None,
            "mean": round(statistics.fmean(latencies), 3) if latencies else None,
            "p50": round(percentile(latencies, 0.50), 3) if latencies else None,
            "p95": round(percentile(latencies, 0.95), 3) if latencies else None,
            "p99": round(percentile(latencies, 0.99), 3) if latencies else None,
            "max": round(max(latencies), 3) if latencies else None,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--direct-url", default="http://localhost:8081/users/u-123")
    parser.add_argument("--gateway-url", default="http://localhost:8080/users/u-123")
    parser.add_argument("--requests", type=int, default=100, help="Số request đo cho mỗi đường gọi")
    parser.add_argument("--warmup", type=int, default=5, help="Số request làm nóng mỗi đường gọi")
    parser.add_argument(
        "--rate",
        type=float,
        default=5,
        help="Tốc độ tuần tự theo request/giây (tối đa 9 để tránh giới hạn 10 r/s)",
    )
    parser.add_argument("--timeout", type=float, default=5, help="Timeout cho mỗi request, tính bằng giây")
    parser.add_argument("--output", help="Đường dẫn JSON để lưu kết quả; mặc định chỉ in ra terminal")
    args = parser.parse_args()

    if args.requests < 1 or args.warmup < 0 or args.timeout <= 0:
        parser.error("requests phải dương, warmup không âm và timeout phải dương")
    if not 0 < args.rate <= 9:
        parser.error("rate phải lớn hơn 0 và không quá 9 request/giây")
    token = os.environ.get("JWT_TOKEN", "").strip()
    if not token:
        parser.error("Hãy đặt JWT_TOKEN trong môi trường trước khi chạy benchmark")

    configuration = {
        "measured_at_utc": datetime.now(timezone.utc).isoformat(),
        "python_version": platform.python_version(),
        "platform": platform.platform(),
        "request_count_per_path": args.requests,
        "warmup_per_path": args.warmup,
        "offered_rate_per_path": args.rate,
        "client_concurrency": 1,
        "timeout_seconds": args.timeout,
        "route": "GET /users/u-123",
        "direct": run_series(
            args.direct_url, None, args.warmup, args.requests, args.rate, args.timeout
        ),
        "gateway": run_series(
            args.gateway_url, token, args.warmup, args.requests, args.rate, args.timeout
        ),
    }

    direct_p50 = configuration["direct"]["latency_ms"]["p50"]
    gateway_p50 = configuration["gateway"]["latency_ms"]["p50"]
    direct_p95 = configuration["direct"]["latency_ms"]["p95"]
    gateway_p95 = configuration["gateway"]["latency_ms"]["p95"]
    configuration["gateway_overhead_ms"] = {
        "p50": round(gateway_p50 - direct_p50, 3)
        if gateway_p50 is not None and direct_p50 is not None
        else None,
        "p95": round(gateway_p95 - direct_p95, 3)
        if gateway_p95 is not None and direct_p95 is not None
        else None,
    }

    output = json.dumps(configuration, indent=2, ensure_ascii=False)
    print(output)
    if args.output:
        output_path = os.path.abspath(args.output)
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as result_file:
            result_file.write(output + "\n")
        print(f"Kết quả đã lưu tại: {output_path}", file=sys.stderr)

    has_errors = any(
        result["successful"] != result["requests"]
        for result in (configuration["direct"], configuration["gateway"])
    )
    return 1 if has_errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
