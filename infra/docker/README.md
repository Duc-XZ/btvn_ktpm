# Local services and API Gateway

Build and start both Spring Boot services and Nginx from the repository root:

```sh
docker compose -f infra/docker/compose.yaml up --build
```

The gateway listens on `http://localhost:8080`:

- `GET /health` — gateway status.
- `/users` and `/users/**` — routed to User Service.
- `/orders` and `/orders/**` — routed to Order Service.

For local development and the later direct-vs-gateway benchmark, the backend ports are also bound to loopback only:

- User Service: `http://localhost:8081`
- Order Service: `http://localhost:8082`

Stop the stack with `docker compose -f infra/docker/compose.yaml down`. JWT checks, rate limiting, dashboard aggregation, and MinIO are added in later steps.
