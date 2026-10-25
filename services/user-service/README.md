# User Service

Spring Boot REST service for user records. This step uses an in-memory store; its contents reset when the process restarts.

## Run

Requirements: Java 21 and Maven 3.6.3 or later.

JWT verification requires `JWT_SECRET` to be set to at least 32 UTF-8 bytes before startup. For local Docker usage, follow [`infra/docker/README.md`](../../infra/docker/README.md). When running this service with Maven directly, set the same environment variable in the shell first.

```sh
mvn spring-boot:run
```

The service listens on `http://localhost:8081`. See [`docs/service-api.md`](../../docs/service-api.md) for the API contract.

## Endpoints

- `GET /health`
- `GET /users` and `GET /users/{userId}`
- `GET /dashboard/users/{userId}` — user plus that user's orders
- `POST /users`, `PUT /users/{userId}`, `DELETE /users/{userId}`
