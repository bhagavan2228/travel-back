# AI Travel Backend

Spring Boot REST API for the Voyager travel application.

## Database (MySQL Workbench)

- **Host:** `127.0.0.1`
- **Port:** `3306`
- **Database:** `travelback`
- **User:** `root`

### Connect with your MySQL password

1. Copy `src/main/resources/application-local.yml.example` to `application-local.yml`
2. Set your MySQL root password:

```yaml
spring:
  datasource:
    password: YOUR_PASSWORD
```

Or set an environment variable before starting:

```powershell
$env:MYSQL_PASSWORD = "your_password"
mvn spring-boot:run
```

## Quick start (H2 in-memory — no MySQL password needed)

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
```

API base URL: **http://localhost:8082/api** (port 8082 — Oracle uses 8080 on this machine)

## API overview

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/auth/register`, `/auth/login`, `/auth/refresh` | Public |
| GET | `/destinations`, `/destinations/{id}`, `/destinations/search` | Public |
| GET | `/destinations/{id}/food`, `/events`, `/reviews` | Public |
| CRUD | `/trips` | JWT |
| POST | `/bookings` | JWT |
| GET | `/notifications` | JWT |
| POST | `/assistant/chat` | JWT |
| POST | `/toxicity/check` | JWT |

Seeded destinations on first run: Paris, Tokyo, Bali, New York, Santorini.
