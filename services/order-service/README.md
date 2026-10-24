# Order Service

Spring Boot REST service for order records. This step uses an in-memory store; its contents reset when the process restarts.

## Run

Requirements: Java 21 and Maven 3.6.3 or later.

```sh
mvn spring-boot:run
```

The service listens on `http://localhost:8082`. See [`docs/service-api.md`](../../docs/service-api.md) for the API contract.

## Endpoints

- `GET /health`
- `GET /orders` and `GET /orders?userId={userId}`
- `GET /orders/{orderId}`
- `POST /orders`, `PUT /orders/{orderId}`, `DELETE /orders/{orderId}`
