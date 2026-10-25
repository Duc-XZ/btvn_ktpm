# Local services and API Gateway

Tạo secret HS256 và thông tin đăng nhập MinIO dùng riêng cho máy cá nhân, sau đó build và chạy MinIO, hai service Spring Boot cùng Nginx từ thư mục gốc repo. Trong PowerShell:

```powershell
$secretBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($secretBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
$minioBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($minioBytes)
$rng.Dispose()
$env:MINIO_ROOT_USER = "local-minio-admin"
$env:MINIO_ROOT_PASSWORD = [Convert]::ToBase64String($minioBytes)
$env:MINIO_PUBLIC_ENDPOINT = "http://localhost:9000"
$env:MINIO_CORS_ORIGINS = "http://localhost:8080,http://localhost:5173"
docker compose -f infra/docker/compose.yaml up --build -d
mvn -q -f benchmarks/benchmark-runner/pom.xml package
$env:JWT_TOKEN = (java -jar benchmarks/benchmark-runner/target/benchmark-runner-0.1.0-SNAPSHOT.jar --mode=token --subject=u-123)
```

The secret must contain at least 32 UTF-8 bytes and is read only by User Service. Do not commit the secret. The generated token is valid for 15 minutes and can be used immediately against the gateway.

Send the generated token as `Authorization: Bearer <token>`. Missing, malformed, invalid-signature, expired, or not-yet-valid tokens receive `401`. This token helper is for local demonstrations only; it does not authenticate a real user.

For example, keep the token in a PowerShell variable and call through the gateway:

```powershell
$token = $env:JWT_TOKEN
curl.exe -H "Authorization: Bearer $token" http://localhost:8080/users/u-123
```

The gateway listens on `http://localhost:8080`:

- `GET /health` — gateway status.
- `/users` and `/users/**` — routed to User Service.
- `POST /users/{userId}/avatar/upload-url` — issues an upload URL; the file is sent directly to MinIO.
- `/orders` and `/orders/**` — routed to Order Service.
- `/dashboard/users/{userId}` — routed to User Service, which fetches that user's orders.
- Các API `/users`, `/orders` và dashboard đều cần JWT HS256 hợp lệ, đồng thời bị giới hạn 10 request/giây theo IP nguồn. Request vượt mức nhận `429`.

For local development and the later direct-vs-gateway benchmark, the backend ports are also bound to loopback only:

- User Service: `http://localhost:8081`
- Order Service: `http://localhost:8082`
- MinIO API: `http://localhost:9000`; Console: `http://localhost:9001`

To upload an avatar, request a URL with a JWT, then PUT the file bytes to MinIO using the URL and the exact `Content-Type` returned by the API:

```powershell
$token = $env:JWT_TOKEN
$upload = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/users/u-123/avatar/upload-url" -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body '{"contentType":"image/png"}'
Invoke-WebRequest -Method Put -Uri $upload.uploadUrl -InFile ".\avatar.png" -ContentType $upload.requiredHeaders.'Content-Type'
```

The first Compose build compiles MinIO from its source. MinIO data persists in the `minio_data` Docker volume.

Stop the stack with `docker compose -f infra/docker/compose.yaml down`. Avatar data remains in the `minio_data` volume.
