# Local services and API Gateway

Set a local HS256 secret, then build and start both Spring Boot services and Nginx from the repository root. In PowerShell:

```powershell
$secretBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($secretBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
docker compose -f infra/docker/compose.yaml up --build -d
python scripts/generate_demo_jwt.py --subject u-123
```

The secret must contain at least 32 UTF-8 bytes and is read only by User Service. Do not commit the secret. The printed token is valid for 15 minutes and can be used immediately against the gateway.

Send the printed token as `Authorization: Bearer <token>`. Missing, malformed, invalid-signature, expired, or not-yet-valid tokens receive `401`. This token helper is for local demonstrations only; it does not authenticate a real user.

For example, keep the token in a PowerShell variable and call through the gateway:

```powershell
$token = python scripts/generate_demo_jwt.py --subject u-123
curl.exe -H "Authorization: Bearer $token" http://localhost:8080/users/u-123
```

The gateway listens on `http://localhost:8080`:

- `GET /health` — gateway status.
- `/users` and `/users/**` — routed to User Service.
- `/orders` and `/orders/**` — routed to Order Service.
- `/dashboard/users/{userId}` — routed to User Service, which fetches that user's orders.
- Các API `/users`, `/orders` và dashboard đều cần JWT HS256 hợp lệ, đồng thời bị giới hạn 10 request/giây theo IP nguồn. Request vượt mức nhận `429`.

For local development and the later direct-vs-gateway benchmark, the backend ports are also bound to loopback only:

- User Service: `http://localhost:8081`
- Order Service: `http://localhost:8082`

Stop the stack with `docker compose -f infra/docker/compose.yaml down`. Dashboard aggregation and MinIO are added in later steps.
