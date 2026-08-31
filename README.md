# Product API — Zest India Java Backend Assignment

RESTful Product CRUD API built with **Java 17**, **Spring Boot 3.3**, **Spring Data JPA**, **MySQL**, **Spring Security (JWT + refresh-token rotation)**, **JUnit 5 / Mockito**, **OpenAPI / Swagger**, and **Docker Compose**.

## Architecture

The project follows a layered, package-by-feature structure:

```
src/main/java/com/zestindia/productapi
├── auth        Authentication, users, refresh tokens
├── product     Product / Item entities, services, controllers
├── security    JWT filter, user principal, JSON security handlers
├── config      Security, CORS, OpenAPI, async, data seed
└── common      Standardized errors, pagination, async audit
```

Request flow:

```
Client → JwtAuthenticationFilter → Controller → Service → JPA Repository → MySQL
                                      ↓
                              GlobalExceptionHandler
                                      ↓
                              Async AuditService
```

- **Controllers** only map HTTP and validation.
- **Services** own business rules and transactions.
- **Repositories** isolate persistence.
- **DTOs / records** keep the public JSON contract separate from JPA entities.

## API versioning and endpoints

All business APIs are versioned under `/api/v1/`.

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/api/v1/auth/register` | Public | Register a `USER` |
| POST | `/api/v1/auth/login` | Public | Access + refresh tokens |
| POST | `/api/v1/auth/refresh` | Public | Rotate refresh token |
| POST | `/api/v1/auth/logout` | Authenticated | Revoke all refresh tokens |
| GET | `/api/v1/products` | ADMIN, USER | Paginated product list |
| GET | `/api/v1/products/{id}` | ADMIN, USER | Product by id |
| POST | `/api/v1/products` | ADMIN | Create product |
| PUT | `/api/v1/products/{id}` | ADMIN | Update product |
| DELETE | `/api/v1/products/{id}` | ADMIN | Delete product (cascades items) |
| GET | `/api/v1/products/{id}/items` | ADMIN, USER | Items of a product |
| POST | `/api/v1/products/{id}/items` | ADMIN | Add item |
| PUT | `/api/v1/products/{id}/items/{itemId}` | ADMIN | Update item |
| DELETE | `/api/v1/products/{id}/items/{itemId}` | ADMIN | Delete item |

Pagination query parameters on `GET /api/v1/products`: `page`, `size`, `sort` (Spring Data defaults: `page=0`, `size=10`).

Example:

`GET /api/v1/products?page=0&size=10&sort=createdOn,desc`

## Security

- Stateless JWT access tokens (default 15 minutes).
- Refresh tokens stored as **SHA-256 hashes**, never in plaintext.
- **Refresh-token rotation** with family-level reuse detection: presenting a revoked token revokes the whole family.
- Role-based authorization: `ADMIN` can mutate data; `USER` is read-only on products.
- Jakarta Validation on every write request.
- CORS configured from `app.cors.allowed-origins`.
- Security headers include HSTS, `X-Content-Type-Options`, and denied frames.
- HTTPS can be enforced by setting `app.security.require-https=true` (typical production setup is TLS at a reverse proxy such as Nginx or a cloud load balancer).

### Seeded accounts

| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin@123` | ADMIN |
| `user` | `User@123` | USER |

Change these via `app.seed.*` or `APP_JWT_SECRET` before any real deployment.

## Database

Flyway migration `V1__init_schema.sql` creates the assignment tables plus auth tables:

- `product` and `item` match the provided schema (`product_name`, audit columns, `item.quantity`).
- Indexes: `product_name`, `product.created_on`, `item.product_id`, refresh-token lookup, user role.
- `item.product_id` uses `ON DELETE CASCADE`.

Hibernate runs with `ddl-auto: validate` so the schema is owned by Flyway.

## Async processing

`AuditService` logs create / update / delete events on a dedicated `auditExecutor` thread pool so write APIs do not block on audit I/O.

## Local setup (without Docker)

Prerequisites: **JDK 17+** (Java 21/24 also works for tests), **Maven 3.9+** or the included Maven Wrapper, **MySQL 8**.

```sql
CREATE DATABASE productdb;
CREATE USER 'zest'@'%' IDENTIFIED BY 'zestpass';
GRANT ALL PRIVILEGES ON productdb.* TO 'zest'@'%';
FLUSH PRIVILEGES;
```

```bash
./mvnw test
./mvnw spring-boot:run
```

On Windows PowerShell use `.\mvnw.cmd test` and `.\mvnw.cmd spring-boot:run`.

The API listens on `http://localhost:8080`.

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Docker

```bash
docker compose up --build
```

This starts MySQL 8 and the Spring Boot app on port `8080`. Flyway applies the schema on first boot.

Stop and remove volumes:

```bash
docker compose down -v
```

## Quick start with curl

```bash
# Login
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"Admin@123\"}"

# Create product (replace TOKEN)
curl -s -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productName\":\"Wireless Mouse\"}"

# List products
curl -s http://localhost:8080/api/v1/products?page=0&size=10 \
  -H "Authorization: Bearer TOKEN"

# Add item
curl -s -X POST http://localhost:8080/api/v1/products/1/items \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"quantity\":5}"
```

## Error format

All failures use a single JSON shape:

```json
{
  "timestamp": "2026-08-31T16:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/products",
  "errors": [
    { "field": "productName", "message": "must not be blank" }
  ]
}
```

## Testing

Tests use **JUnit 5**, **Mockito**, **Spring Boot Test**, **MockMvc**, and an **H2** in-memory database (`MODE=MySQL`) with Flyway disabled.

```bash
./mvnw test
```

Coverage includes:

- `ProductServiceTest` — unit tests for CRUD, items, and not-found paths
- `RefreshTokenServiceTest` — issue, rotate, and reuse detection
- `ProductControllerTest` — REST mapping, validation, and role checks
- `ProductApiIntegrationTest` — end-to-end auth, RBAC, pagination, and token rotation

## Configuration

| Property | Default | Purpose |
|----------|---------|---------|
| `app.jwt.secret` / `APP_JWT_SECRET` | local demo key | HMAC signing key (use 256+ bits in production) |
| `app.jwt.access-token-expiration-ms` | `900000` | 15 minutes |
| `app.jwt.refresh-token-expiration-ms` | `604800000` | 7 days |
| `app.security.require-https` | `false` | Channel security / HTTPS enforcement |
| `app.cors.allowed-origins` | localhost origins | CORS allow-list |

## Submission

The company accepts **only a public GitHub repository URL** (no ZIP files).

```bash
git init
git add .
git commit -m "Complete Zest India Product API assignment"
```

Then create a **public** repository on GitHub (for example `product-api`), push, and paste that URL into the Google Form along with your name, email, mobile number, years of experience, and time taken.
