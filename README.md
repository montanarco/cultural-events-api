# cultural-events-api

A REST API for managing **Cultural Events**, built as a Spring Boot homework project for the EPAM Java Learning Path.
The project covers the full development lifecycle: CRUD API, JWT security, database migrations, observability with Prometheus, and a comprehensive test suite.

**Repository:** https://github.com/montanarco/cultural-events-api

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Build | Maven (wrapper `mvnw`) |
| Database | H2 in-memory (dev) |
| ORM | Spring Data JPA / Hibernate 7 |
| Migrations | Flyway |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Monitoring | Spring Actuator + Micrometer + Prometheus |
| Utilities | Lombok |

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| Java | 21 |
| Maven | bundled via `mvnw` — no separate install needed |

> This project requires **Java 21**. If it is not your default JDK, set `JAVA_HOME` explicitly before running any Maven command.

---

## Build & Run

**Clone**
```bash
git clone https://github.com/montanarco/cultural-events-api.git
cd cultural-events-api
```

**Run**
```bash
# macOS / Linux
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw spring-boot:run

# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-21
mvnw.cmd spring-boot:run
```

The app starts on **http://localhost:8080**.
On startup Flyway runs two migrations: schema creation (V1) and seed data insertion (V2, 4 sample events).

**Build (skip tests)**
```bash
JAVA_HOME=... ./mvnw clean package -DskipTests
```

---

## Running Tests

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test
```

The test suite has **42 tests** across four layers:

| Layer | Class | What it covers |
|-------|-------|---------------|
| Repository | `CulturalEventRepositoryTest` | JPA CRUD with `@Transactional` rollback isolation |
| Service | `CulturalEventServiceTest` | Business logic and metric recording (pure Mockito) |
| Controller | `CulturalEventControllerTest` | Routing, JSON serialization, role-based access (`@WithMockUser`) |
| Integration | `CulturalEventIntegrationTest` | Full stack with real JWT tokens and Flyway seed data |

> **Note — Spring Boot 4.0 breaking change:** `@WebMvcTest`, `@DataJpaTest`, and `@AutoConfigureMockMvc` were removed. All tests use `@SpringBootTest` with `MockMvcBuilders.webAppContextSetup()`.

---

## Authentication

All `/events` endpoints require a Bearer JWT token.

**1. Get a token**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

**2. Use the token**
```bash
curl http://localhost:8080/events -H "Authorization: Bearer $TOKEN"
```

**Dev users (in-memory)**

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | `admin123` | `ROLE_ADMIN` | Full CRUD |
| `user` | `user123` | `ROLE_USER` | GET only |

> Never use these credentials in production. Override via environment variables or a secrets manager.

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/login` | Public | Returns `{"access_token": "<JWT>"}` |

### Cultural Events

| Method | Endpoint | Role required | Description |
|--------|----------|--------------|-------------|
| `GET` | `/events` | USER or ADMIN | List all events |
| `GET` | `/events/{id}` | USER or ADMIN | Get event by ID |
| `POST` | `/events` | ADMIN | Create a new event |
| `PUT` | `/events/{id}` | ADMIN | Update an event |
| `DELETE` | `/events/{id}` | ADMIN | Delete an event |

**Event type values:** `SPORTS` · `MUSIC` · `ART` · `THEATER` · `CAMPING`

### Actuator (public, no auth required)

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | App health + custom event health indicator |
| `/actuator/info` | App name, version, description |
| `/actuator/metrics` | All registered metric names |
| `/actuator/prometheus` | Prometheus scrape endpoint |
| `/actuator/env` | Environment properties |

---

## Examples

**Create an event (admin)**
```bash
curl -X POST http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Spring Music Festival",
    "type": "MUSIC",
    "date": "2026-05-10",
    "location": "Central Park",
    "address": "123 Main St, Springfield",
    "description": "A festival celebrating local music talent."
  }'
```

**Get all events**
```bash
curl http://localhost:8080/events -H "Authorization: Bearer $TOKEN"
```

**H2 Console (dev only)**

Browse to **http://localhost:8080/h2-console**
JDBC URL: `jdbc:h2:mem:cultural_events_db` · Username: `sa` · Password: *(empty)*

---

## Monitoring with Prometheus

The app exposes custom business metrics at `/actuator/prometheus`. Run Prometheus with:

```bash
docker run -d --name prometheus -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

Sample `prometheus.yml`:
```yaml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: 'cultural-events-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

See `prometheus-monitoring.md` for a full report of available metrics and example queries.

---

## Configuration

The base configuration lives in `src/main/resources/application.properties`.
Profile-specific files (`application-dev.properties`, `application-stg.properties`, `application-prod.properties`) are **gitignored** — create them locally from the template below.

**Template for profile configs:**
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=none
```

**JWT secret** — override the dev default in production:
```bash
export JWT_SECRET=<your-base64-encoded-256-bit-secret>
```

---

## Future Improvements

This project is a solid foundation for a production Spring Boot service. Suggested next steps:

### Security
- Replace in-memory users with a database-backed `UserDetailsService` and a `users` table.
- Add refresh token support and token revocation (blacklist via Redis or DB).
- Enable HTTPS / TLS termination at the reverse proxy level.
- Add rate limiting on `/auth/login` to harden against brute-force attacks.

### API & Code Structure
- Add `@ControllerAdvice` with a global exception handler (return consistent error JSON for 404, 400, 403).
- Add request validation with `@Valid` / `jakarta.validation` constraints on the `CulturalEvent` model.
- Introduce a DTO layer (`CulturalEventRequest` / `CulturalEventResponse`) to decouple the API contract from the JPA entity.
- Paginate `GET /events` with `Pageable` to handle large datasets.

### Performance
- Replace H2 with PostgreSQL for staging/production.
- Add a second-level Hibernate cache (e.g., Caffeine) for read-heavy workloads.
- Profile slow queries with `spring.jpa.show-sql=true` + `p6spy`.

### Configuration & Deployment
- Introduce Spring Cloud Config or use HashiCorp Vault for secrets management.
- Add a `Dockerfile` and `docker-compose.yml` (app + PostgreSQL + Prometheus + Grafana).
- Set up a CI/CD pipeline (GitHub Actions) to run tests and build the Docker image on every push.

### Testing
- Add contract tests with Spring Cloud Contract or Pact.
- Increase integration test isolation with `@Sql` scripts for deterministic seed data.
- Add load/performance tests with Gatling or k6.

### Documentation
- Add OpenAPI / Swagger UI with `springdoc-openapi`.
- Generate API client SDKs from the OpenAPI spec.

---

## Project Structure

```
src/
├── main/java/com/epam/java_learning_epam/
│   ├── config/          # SecurityConfig, JwtAuthFilter
│   ├── controller/      # CulturalEventController, AuthController
│   ├── model/           # CulturalEvent (JPA entity), EventType, LoginRequest
│   ├── monitor/         # CustomEventHealthIndicator, EventMetricsService
│   ├── repository/      # CulturalEventRepository
│   └── service/         # CulturalEventService, JwtService
└── main/resources/
    ├── application.properties
    └── db/migration/    # V1__init.sql, V2__add_sample_data.sql
```
