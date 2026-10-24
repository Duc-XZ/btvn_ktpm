# Service API contract (step 2)

Each service is an independent Spring Boot application. All responses use JSON. Validation failures return `400 Bad Request`; unknown IDs return `404 Not Found`. Public user and order requests go through the gateway. User Service also exposes `GET /internal/auth/verify` for Nginx auth subrequests; it returns `204` for a valid token or `401` otherwise.

## User Service

Base URL: `http://localhost:8081`

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/health` | Service status. |
| `GET` | `/users` | List users. |
| `GET` | `/users/{userId}` | Find a user. |
| `POST` | `/users` | Create a user; returns `201` and `Location`. |
| `PUT` | `/users/{userId}` | Replace a user's name, email, and avatar key. |
| `DELETE` | `/users/{userId}` | Delete a user; returns `204`. |

Create/update request:

```json
{
  "name": "Linh Nguyen",
  "email": "linh@example.com",
  "avatarKey": null
}
```

User response:

```json
{
  "id": "u-123",
  "name": "Linh Nguyen",
  "email": "linh@example.com",
  "avatarKey": null
}
```

## Order Service

Base URL: `http://localhost:8082`

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/health` | Service status. |
| `GET` | `/orders` | List all orders. |
| `GET` | `/orders?userId={userId}` | List a user's orders; used by dashboard aggregation later. |
| `GET` | `/orders/{orderId}` | Find an order. |
| `POST` | `/orders` | Create an order; returns `201` and `Location`. |
| `PUT` | `/orders/{orderId}` | Replace an order's user, status, and total. |
| `DELETE` | `/orders/{orderId}` | Delete an order; returns `204`. |

Create/update request:

```json
{
  "userId": "u-123",
  "status": "NEW",
  "total": 125000.00
}
```

Order response:

```json
{
  "id": "o-456",
  "userId": "u-123",
  "status": "NEW",
  "total": 125000.00
}
```

## Local sample data

User Service seeds `u-123` and `u-456`. Order Service seeds three orders, including two for `u-123`. Data is held in process memory and resets when a service restarts; persistence is outside this step.
