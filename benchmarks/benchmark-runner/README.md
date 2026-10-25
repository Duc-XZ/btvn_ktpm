# Benchmark runner (Java Spring Boot)

Ứng dụng dòng lệnh này tạo JWT demo hoặc đo độ trễ của một endpoint gọi trực tiếp và endpoint tương ứng qua API Gateway. Đây là công cụ Java riêng, không phải service chạy liên tục.

## Build

Yêu cầu JDK 21 và Maven:

```powershell
mvn -q -f benchmarks/benchmark-runner/pom.xml package
```

## Tạo JWT demo

Đặt `JWT_SECRET` giống giá trị cấu hình cho User Service, tối thiểu 32 byte UTF-8:

```powershell
$env:JWT_TOKEN = java -jar benchmarks/benchmark-runner/target/benchmark-runner-0.1.0-SNAPSHOT.jar --mode=token --subject=u-123
```

JWT mặc định có hạn 15 phút. Có thể đổi thời hạn bằng `--ttl-seconds=900`.

## Chạy benchmark

Khởi động các service và Gateway trước. Đặt `JWT_TOKEN` thành token có quyền gọi User Service, sau đó chạy:

```powershell
java -jar benchmarks/benchmark-runner/target/benchmark-runner-0.1.0-SNAPSHOT.jar --mode=benchmark --requests=100 --warmup=5 --rate=5 --output=benchmarks/results/local.json
```

Mặc định gọi trực tiếp `http://localhost:8081/users/u-123` và qua Gateway `http://localhost:8080/users/u-123`. Có thể đổi URL với `--direct-url=` và `--gateway-url=`. Tốc độ tối đa cho phép là 9 request/giây để tránh vượt ngưỡng 10 request/giây của Gateway. Kết quả gồm mã trạng thái, lỗi kết nối, throughput và p50/p95/p99; JWT không được ghi vào kết quả.
