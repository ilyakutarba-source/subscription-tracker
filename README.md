# Subscription Tracker

Spring Boot REST backend for recording recurring subscriptions, seeing upcoming charges, and calculating monthly/yearly spending without mixing currencies.

## Technology stack

- Java 21, Maven Wrapper, Spring Boot 4.1
- Spring Web, Data JPA, Bean Validation, Actuator
- PostgreSQL 17 and Flyway
- JUnit, Mockito, MockMvc
- Multi-stage Docker build and Docker Compose

The concise product decision record and task breakdown are in [docs/PRODUCT_AND_IMPLEMENTATION_PLAN.md](docs/PRODUCT_AND_IMPLEMENTATION_PLAN.md).

## Architecture

```text
Request -> Controller -> DTO validation -> Service -> Repository -> PostgreSQL
Response <- response DTO <- Mapper <------------------------------+
```

Controllers handle HTTP only. Services own lifecycle and calculation rules. Repositories only read/write data. Flyway owns the database schema; `ddl-auto=validate` detects drift without creating or deleting production data.

## Prerequisites

- JDK 21
- Docker with Compose (for PostgreSQL and container workflows)

## Local development

Start PostgreSQL only:

```bash
docker compose up -d postgres
```

Run the application from the IDE with the `local` profile, or:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Local defaults are `jdbc:postgresql://localhost:5432/subscription_tracker`, user `subscription_tracker`, password `subscription_tracker`. Override them with the environment variables below.

## Tests and build

```bash
./mvnw clean verify
```

Windows:

```powershell
.\mvnw.cmd clean verify
```

Unit/MockMvc tests cover creation, normalization, monthly/yearly conversion, active-only totals, currency separation, valid and invalid POST, missing resources, cancellation and dashboard output.

## API

All application endpoints use `/api/v1`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/subscriptions` | List subscriptions |
| GET | `/api/v1/subscriptions/{id}` | Get one subscription |
| POST | `/api/v1/subscriptions` | Create a subscription |
| PUT | `/api/v1/subscriptions/{id}` | Replace editable fields |
| PATCH | `/api/v1/subscriptions/{id}/cancel` | Set status to `CANCELLED` |
| DELETE | `/api/v1/subscriptions/{id}` | Permanently delete a record |
| GET | `/api/v1/dashboard?days=30` | Costs and active payments due in the next 1–365 days |
| GET | `/actuator/health` | Health status |

Create example:

```json
{
  "name": "ChatGPT",
  "description": "AI assistant",
  "price": 20.00,
  "currency": "USD",
  "billingPeriod": "MONTHLY",
  "startDate": "2026-01-01",
  "nextPaymentDate": "2026-02-01",
  "category": "SOFTWARE"
}
```

Omitting `status` on create defaults it to `ACTIVE`. Supported billing periods: `MONTHLY`, `YEARLY`. Supported statuses: `ACTIVE`, `PAUSED`, `CANCELLED`. Categories: `ENTERTAINMENT`, `SOFTWARE`, `EDUCATION`, `CLOUD`, `MUSIC`, `GAMING`, `OTHER`.

Validation errors share one format:

```json
{
  "timestamp": "2026-09-17T12:00:00Z",
  "status": 400,
  "error": "Validation failed",
  "message": "One or more fields are invalid",
  "path": "/api/v1/subscriptions",
  "fieldErrors": { "name": "must not be blank" }
}
```

## Environment variables

| Variable | Required in production | Example |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` |
| `DB_URL` | yes | `jdbc:postgresql://postgres-host:5432/subscription_tracker` |
| `DB_USERNAME` | yes | `subscription_tracker` |
| `DB_PASSWORD` | yes | a secret value |
| `SERVER_PORT` | no | `8080` |

Never commit `.env` or production credentials.

## Docker

Build the production image:

```bash
docker build -t subscription-tracker .
```

Run PostgreSQL only for IDE/Maven development:

```bash
docker compose up -d postgres
```

Run the full stack:

```bash
docker compose --profile full up --build
```

Check health:

```bash
curl http://localhost:8080/actuator/health
```

Stop containers while preserving the named database volume:

```bash
docker compose --profile full down
```

Do not add `-v` unless you intentionally want to delete local database data.

## GitHub workflow

The repository uses branch `main`. Before pushing:

```bash
./mvnw clean verify
docker build -t subscription-tracker .
docker compose --profile full up --build -d
curl http://localhost:8080/actuator/health
docker compose --profile full down
git status
```

If GitHub CLI is unavailable, create an empty `subscription-tracker` repository in GitHub, then run:

```bash
git remote add origin <GITHUB_REPOSITORY_URL>
git branch -M main
git push -u origin main
```

## Deployment with Dokploy

1. Create a PostgreSQL service/database in Dokploy. Use persistent storage and do not expose port 5432 publicly.
2. Connect the GitHub `subscription-tracker` repository and select branch `main`.
3. Select Dockerfile deployment; context and Dockerfile path are the repository root and `Dockerfile`.
4. Set application/container port to `8080`.
5. Configure:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<dokploy-postgres-host>:5432/<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

6. Deploy. Startup connects to PostgreSQL, runs Flyway, validates the JPA mapping, and then accepts traffic.
7. Configure the platform health check as `/actuator/health`; success is `{"status":"UP"}`.

No VPS firewall, SSH, Docker daemon, reverse proxy, system port or existing container changes are required by this repository.

## Database lifecycle

Migration `V1__create_subscriptions.sql` creates the `subscriptions` table, checks positive prices and supported enum names, and indexes status/upcoming-payment lookups. Future schema changes must be new versioned Flyway migrations. Production never uses `ddl-auto=create`, destructive migrations, or automatic data removal.

