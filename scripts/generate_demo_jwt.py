#!/usr/bin/env python3
"""Create a short-lived HS256 token for local gateway demonstrations."""

import argparse
import base64
import hashlib
import hmac
import json
import os
import sys
import time


def base64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--subject", default="u-123", help="JWT sub claim (default: u-123)")
    parser.add_argument("--ttl-seconds", type=int, default=900, help="Token lifetime (default: 900)")
    args = parser.parse_args()

    secret = os.environ.get("JWT_SECRET", "").encode("utf-8")
    if len(secret) < 32:
        print("JWT_SECRET must be set to at least 32 UTF-8 bytes", file=sys.stderr)
        return 2
    if args.ttl_seconds < 1:
        print("--ttl-seconds must be greater than zero", file=sys.stderr)
        return 2

    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {"sub": args.subject, "iat": now, "exp": now + args.ttl_seconds}
    encoded_header = base64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    encoded_payload = base64url(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    signing_input = f"{encoded_header}.{encoded_payload}".encode("ascii")
    signature = hmac.new(secret, signing_input, hashlib.sha256).digest()
    print(f"{encoded_header}.{encoded_payload}.{base64url(signature)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
