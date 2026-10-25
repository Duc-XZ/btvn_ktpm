# Benchmark độ trễ: gọi thẳng và qua Gateway

Benchmark gọi cùng endpoint `GET /users/u-123` theo hai đường:

- Trực tiếp tới User Service (`localhost:8081`).
- Qua Nginx Gateway (`localhost:8080`), có JWT và auth subrequest.

Hai lượt chạy tuần tự với cùng số request, tốc độ và timeout. Mặc định dùng 5 request/giây, 100 request đo và 5 request làm nóng cho mỗi đường. Tốc độ tối đa cho phép là 9 request/giây để tránh chạm rate limit 10 request/giây ở gateway.

## Chạy trên Windows PowerShell

Khởi động stack theo hướng dẫn trong [`infra/docker/README.md`](../infra/docker/README.md), rồi làm theo các bước sau trong cùng terminal:

Build ứng dụng Java Spring Boot một lần:

```powershell
mvn -q -f benchmarks/benchmark-runner/pom.xml package
```

Tạo JWT demo và chạy benchmark (giữ tốc độ dưới giới hạn 10 request/giây của Gateway):

```powershell
$env:JWT_TOKEN = (java -jar benchmarks/benchmark-runner/target/benchmark-runner-0.1.0-SNAPSHOT.jar --mode=token --subject=u-123)
java -jar benchmarks/benchmark-runner/target/benchmark-runner-0.1.0-SNAPSHOT.jar --mode=benchmark --requests=100 --warmup=5 --rate=5 --output=benchmarks/results/local.json
```

Công cụ in và lưu JSON gồm p50/p95/p99, throughput thành công, mã trạng thái, lỗi kết nối và chênh lệch p50/p95 giữa gateway với gọi trực tiếp. Token không được ghi vào kết quả.

## Cách đọc kết quả

`gatewayMinusDirectP50Ms` và `gatewayMinusDirectP95Ms` là độ trễ gateway trừ độ trễ gọi trực tiếp. Các giá trị này bao gồm Nginx, xác minh JWT qua auth subrequest và chuyển tiếp HTTP. Vì User Service giữ dữ liệu trong bộ nhớ, phép đo tập trung vào overhead của hai đường gọi, không đại diện cho tải cơ sở dữ liệu hay tải nhiều người dùng.

Benchmark chạy tuần tự với một client để loại ảnh hưởng của concurrency và tránh rate limit. Kết quả phụ thuộc máy chạy, trạng thái Docker, CPU và thời điểm đo. Hãy lưu nhiều lần chạy nếu cần so sánh ổn định hơn.
