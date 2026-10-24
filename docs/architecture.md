# Kiến trúc hệ thống User–Order

## Mục tiêu

Hệ thống gồm hai backend service (`User Service`, `Order Service`) và một API Gateway (Nginx). Gateway định tuyến request, xác thực JWT và giới hạn tốc độ. Một endpoint dashboard trả về thông tin user cùng các đơn hàng của user đó. User Service cấp pre-signed URL để trình duyệt tải ảnh trực tiếp lên MinIO; URL có hiệu lực 5 phút.

## Thành phần và trách nhiệm

| Thành phần | Trách nhiệm |
| --- | --- |
| Nginx API Gateway | Điểm vào duy nhất; định tuyến `/users` và `/orders`; gọi auth subrequest để kiểm tra JWT; giới hạn tối đa 10 request/giây theo IP nguồn; chuyển tiếp request đến backend. |
| User Service | CRUD/thông tin user; xác minh JWT cho auth subrequest nội bộ; endpoint dashboard; tạo pre-signed URL MinIO cho ảnh đại diện. |
| Order Service | CRUD/truy vấn đơn hàng; lọc đơn hàng theo `userId`. |
| MinIO | Lưu trữ object ảnh; client tải file trực tiếp bằng URL đã ký. |
| Benchmark | So sánh độ trễ khi gọi endpoint trực tiếp và qua gateway trong cùng điều kiện. |

Dashboard được đặt trong User Service để giữ đúng quy mô hai backend service. Service này lấy user và gọi Order Service theo `userId`, sau đó ghép thành một response. Nginx chỉ định tuyến request, không thực hiện business aggregation.

## Luồng request

```text
Client ──> Nginx Gateway ──> User Service ──> Order Service
                  │                └────────> MinIO (ký URL)
                  └──────────────> Order Service
```

### Routing

- `/users/**` → User Service.
- `/orders/**` → Order Service.
- `/dashboard/users/{userId}` → User Service (qua gateway), nơi tổng hợp user và orders.
- Backend vẫn có địa chỉ nội bộ để benchmark trực tiếp; production client chỉ dùng gateway.

### JWT và giới hạn tốc độ

- Client gửi `Authorization: Bearer <JWT>`.
- Nginx gọi endpoint xác minh nội bộ của User Service bằng `auth_request`; chỉ proxy request API khi endpoint trả `2xx`.
- User Service xác minh chữ ký HS256, `sub`, `exp`, và `nbf` nếu có. Token thiếu, sai chữ ký, hết hạn hoặc chưa có hiệu lực trả `401`.
- Nginx giới hạn 10 request/giây cho mỗi IP nguồn, không cho phép burst và trả `429` khi vượt giới hạn.
- Token ký bằng HS256 cho môi trường demo; secret lấy từ biến môi trường, không commit secret vào repo.
- `JWT_SECRET` tối thiểu 32 byte được truyền bằng biến môi trường; không lưu secret trong Git.

### Dashboard aggregation

Endpoint dự kiến: `GET /dashboard/users/{userId}`.

Response dự kiến:

```json
{
  "user": { "id": "u-123", "name": "...", "email": "..." },
  "orders": [{ "id": "o-456", "userId": "u-123", "status": "..." }]
}
```

User Service gọi Order Service bằng địa chỉ nội bộ, truyền `userId`, ghép kết quả và xử lý trường hợp Order Service lỗi/timeout.

### Upload avatar bằng Valet Key

1. Client đã xác thực gọi User Service để xin URL tải ảnh.
2. User Service tạo pre-signed `PUT` URL cho object key thuộc user đó, TTL 300 giây.
3. Client tải ảnh trực tiếp lên MinIO bằng URL; binary không đi xuyên gateway.
4. Client lưu/nhận lại object key hoặc URL hiển thị theo cấu hình bucket.

## Đo độ trễ

Đo cùng endpoint và dữ liệu cho hai đường gọi:

1. Client → Nginx → backend.
2. Client → backend trực tiếp (chỉ dùng trong môi trường benchmark nội bộ).

Ghi lại số request, concurrency, p50/p95/p99, throughput và lỗi. Tài liệu kết quả phải ghi rõ máy chạy benchmark, thời gian chạy, cấu hình và liệu lượt đo có bao gồm xác thực JWT hay không.

## Cấu trúc thư mục

```text
services/user-service/   # User API, dashboard aggregation, cấp URL MinIO
services/order-service/  # Order API
gateway/nginx/           # Cấu hình Nginx, routing, JWT và rate limit
gateway/aggregation/     # Hợp đồng/ghi chú aggregation; logic chạy trong User Service
infra/docker/            # Cấu hình chạy cục bộ các thành phần
infra/minio/             # Cấu hình MinIO và bucket cục bộ
benchmarks/              # Kịch bản và kết quả đo độ trễ
docs/                    # Kiến trúc, API và hướng dẫn
scripts/                 # Lệnh hỗ trợ phát triển cục bộ
```

## Lộ trình triển khai theo commit nhỏ

1. `docs: document service architecture and implementation plan` — tài liệu kiến trúc này và bộ khung thư mục.
2. `feat: add user and order service skeletons` — hai service tối thiểu, health check và hợp đồng API.
3. `feat: route user and order APIs through nginx` — cấu hình gateway và chạy cục bộ.
4. `feat: validate JWT and rate limit gateway requests` — JWT, giới hạn tốc độ và các phản hồi lỗi.
5. `feat: aggregate user dashboard with orders` — endpoint dashboard trong User Service.
6. `feat: issue five-minute MinIO avatar upload URLs` — cấu hình MinIO và pre-signed URL.
7. `perf: add direct and gateway latency benchmarks` — kịch bản đo và cách đọc kết quả.

Mỗi chặng sẽ được hoàn thiện và review riêng trước khi chuyển sang chặng tiếp theo; không gộp toàn bộ dự án vào một commit.
