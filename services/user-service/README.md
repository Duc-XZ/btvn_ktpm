# User Service

Spring Boot REST service for user records. This step uses an in-memory store; its contents reset when the process restarts.

## Run

Requirements: Java 21 and Maven 3.6.3 or later.

```sh
mvn spring-boot:run
```

The service listens on `http://localhost:8081`. See [`docs/service-api.md`](../../docs/service-api.md) for the API contract.

## Endpoints

- `GET /health`
- `GET /users` and `GET /users/{userId}`
- `POST /users`, `PUT /users/{userId}`, `DELETE /users/{userId}`
