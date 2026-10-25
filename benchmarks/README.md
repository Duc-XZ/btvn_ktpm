# Benchmark độ trễ: gọi thẳng và qua Gateway

Benchmark gọi cùng endpoint `GET /users/u-123` theo hai đường:

- Trực tiếp tới User Service (`localhost:8081`).
- Qua Nginx Gateway (`localhost:8080`), có JWT và auth subrequest.

Hai lượt chạy tuần tự với cùng số request, tốc độ và timeout. Mặc định dùng 5 request/giây, 100 request đo và 5 request làm nóng cho mỗi đường. Tốc độ tối đa của script là 9 request/giây để tránh chạm rate limit 10 request/giây ở gateway.

## Chạy trên Windows PowerShell

Khởi động stack theo hướng dẫn trong [`infra/docker/README.md`](../infra/docker/README.md). Trong cùng terminal, tạo token rồi chạy:

```powershell
$env:JWT_TOKEN = (python scripts/generate_demo_jwt.py --subject u-123)
python benchmarks/benchmark_gateway.py --requests 100 --warmup 5 --rate 5 --output benchmarks/results/local.json
```

Script in và lưu JSON gồm p50/p95/p99, trung bình, throughput, mã trạng thái, lỗi và chênh lệch p50/p95 giữa gateway với gọi trực tiếp. Token không được ghi vào kết quả.

## Cách đọc kết quả

`gateway_overhead_ms` là độ trễ gateway trừ độ trễ gọi trực tiếp. Giá trị này bao gồm Nginx, xác minh JWT qua auth subrequest và chuyển tiếp HTTP. Vì User Service giữ dữ liệu trong bộ nhớ, phép đo tập trung vào overhead của hai đường gọi, không đại diện cho tải cơ sở dữ liệu hay tải nhiều người dùng.

Benchmark chạy tuần tự với một client để loại ảnh hưởng của concurrency và tránh rate limit. Kết quả phụ thuộc máy chạy, trạng thái Docker, CPU và thời điểm đo. Hãy lưu nhiều lần chạy nếu cần so sánh ổn định hơn.
