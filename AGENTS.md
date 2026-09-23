Start every task by reading AGENTS.md.

Then inspect only the files relevant to the current task.

Do not recursively reread the entire repository unless the task genuinely requires it.

Treat the current implementation as the source of truth unless the user explicitly asks for a redesign.

## Project

Subscription Tracker is a Java 21 Spring Boot application for recording recurring subscriptions, viewing upcoming payments, and calculating monthly and yearly spending without combining currencies. It is a small single-service learning project. The backend MVP and a server-rendered web UI are implemented in one artifact and tested; the existing backend deployment runs through Dokploy.

The responsive Thymeleaf web interface is available at `/` and `/subscriptions`. The JSON API remains under `/api/v1`, with Actuator health endpoints unchanged.

## Product scope

Implemented scope:

- account registration, BCrypt password hashing, session login, and logout;
- per-user subscription ownership and IDOR-safe access control;
- role-protected administration for user roles, account blocking, and service totals;
- create, list, retrieve, replace, cancel, and permanently delete subscriptions;
- responsive dashboard and subscription-management web interface;
- dashboard totals for active subscriptions;
- separate monthly and yearly spending totals per currency;
- active upcoming payments for a configurable 1-365 day window;
- PostgreSQL persistence, Flyway schema migration, validation, structured API errors, and health reporting.

Current out of scope:

- password reset, email verification, 2FA, OAuth, and JWT;
- separate SPA or mobile application;
- banking/payment-provider integrations and automatic transaction detection;
- notifications, Telegram integration, AI features, and external schedulers;
- microservices and distributed infrastructure.

## Current state

### Completed

- The backend MVP and all controller mappings listed below are implemented.
- The web UI provides dashboard, list, create, edit, cancel, delete, validation, confirmation, and empty/error states.
- Spring Security form login, signup, logout, database-backed users, and user-scoped REST/Web flows are implemented.
- The `/admin` web area is restricted to `ADMIN` accounts and provides user roles, blocking controls, per-user subscription counts, and service totals.
- Existing pre-account subscriptions are assigned by V2 to a disabled legacy owner that can be activated once through bootstrap environment variables.
- PostgreSQL schema migrations V1-V2, JPA validation, local/prod profiles, Docker image, and Compose stack exist.
- Unit/service, standalone MockMvc, and Spring Security MVC slice tests cover REST, rendered web, signup/login/CSRF, ownership, and dashboard isolation flows.
- The Spring Boot 4 Flyway integration uses `spring-boot-starter-flyway` plus `flyway-database-postgresql`.

### In progress

Nothing is currently marked in progress in the repository.

### Known issues

- Full runtime browser verification against persisted data still depends on a correctly configured PostgreSQL environment.
- There are no repository/database integration tests and no Testcontainers dependency.
- The Dockerfile packages with `-DskipTests`; tests must be run as a separate required check before a production build.
- No CI workflow is stored in this repository.

### Next logical tasks

- Add PostgreSQL integration coverage with Testcontainers if database-level verification is needed.
- Add CI for JDK 21 Maven verification and Docker image building.
- Add browser automation only if ongoing end-to-end UI regression coverage is needed.
- Extend the API only from a concrete product requirement; the current MVP itself is complete.

### Deployment

- Production deployment exists and has previously completed successfully through Dokploy on a VPS.
- The source is the GitHub repository and deployment-sensitive branch `main`; Dokploy builds the root `Dockerfile`.
- The deployment uses a Spring Boot container connected to a PostgreSQL service through Dokploy's internal network. PostgreSQL was configured without a public port.
- Flyway migration V1 has run against the production database and must be treated as production-applied.
- A previous deployment session verified `/actuator/health` and the subscriptions API successfully. This is historical verification, not a permanent uptime guarantee; check Dokploy and the endpoint again whenever current runtime state matters.
- Some production runtime configuration lives in Dokploy and is intentionally not stored in the repository. Verify domains, service IDs, reverse-proxy settings, current containers, environment-variable values, and live status directly in Dokploy when a task depends on them.

## Tech stack

- Java 21
- Spring Boot 4.1.1
- Maven 3.9.11 via Maven Wrapper 3.3.4
- Spring Web, Spring Security, Thymeleaf, Spring Data JPA, Bean Validation, Spring Boot Actuator
- PostgreSQL 17 and Flyway 12.x through the Spring Boot Flyway starter
- JUnit 5, Mockito, AssertJ, and standalone MockMvc
- Docker multi-stage builds and Docker Compose

Lombok and Testcontainers are not used.

## Architecture

```text
HTTP request
  -> Spring Security / authenticated HTTP session
  -> REST or Web MVC controller / DTO or form validation
  -> Service / current user / ownership and business rules
  -> Spring Data repository
  -> PostgreSQL

Entity
  -> manual mapper
  -> response DTO or Thymeleaf view model
  -> JSON response or server-rendered HTML
```

Architectural boundaries:

- Controllers handle HTTP mapping, status codes, parameter validation, and DTO conversion only.
- Services own current-user resolution, ownership enforcement, lifecycle rules, normalization, calculations, and transaction boundaries.
- Repositories own persistence queries; user-facing subscription queries must include the current user's id.
- JPA entities are not exposed directly through REST; records in `api.dto` define API contracts.
- `SubscriptionMapper` performs explicit entity-to-response conversion.
- Flyway owns schema evolution; Hibernate uses `ddl-auto=validate` and must not create production schema.
- `GlobalExceptionHandler` centralizes the currently supported API errors.

## Package structure

```text
com.example.subscriptiontracker
├── api/          REST controllers and the manual response mapper
│   └── dto/      request/response record contracts
├── domain/       JPA entity and domain enums
├── repository/   Spring Data JPA persistence interface
├── security/     Spring Security configuration, principal, database user lookup, auth events, and REST handlers
├── service/      lifecycle and dashboard business logic
├── web/          Thymeleaf MVC controller, form model, formatting, and web error advice
└── exception/    API error record, domain exception, global REST advice
```

Templates live in `src/main/resources/templates`, with local CSS and minimal JavaScript in `static`. There is no separate frontend build or `config` package.

## Domain model

`User` maps to `users` and owns subscriptions.

- Identity: application-generated UUID; the fixed disabled legacy-owner UUID is reserved for V2 backfill.
- Login identifier: normalized lowercase unique email.
- Credentials: BCrypt `passwordHash`; raw passwords are never persisted or logged.
- Profile/security fields: display name, `USER`/`ADMIN` string role, enabled flag, and audit timestamps.
- Authentication uses a database-backed `UserDetailsService`, an `AccountPrincipal`, form login, and an in-memory HTTP session.

`Subscription` maps to `subscriptions`.

- Identity: application-generated UUID.
- Ownership: required lazy `ManyToOne` from `Subscription` to `User`; public requests never accept a user id.
- Descriptive fields: name (maximum 120 characters) and optional description (maximum 1000).
- Money: positive `BigDecimal` with database precision 19 and scale 2; ISO currency is stored separately as an uppercase three-letter string.
- Billing: `MONTHLY` or `YEARLY`; start and next-payment dates use `LocalDate`.
- Category: `ENTERTAINMENT`, `SOFTWARE`, `EDUCATION`, `CLOUD`, `MUSIC`, `GAMING`, or `OTHER`.
- Status: `ACTIVE`, `PAUSED`, or `CANCELLED`.
- Audit fields: `createdAt` and `updatedAt` are `Instant` values populated by JPA callbacks.
- Enums are persisted with `EnumType.STRING`; their stored names are part of the schema/API contract.

## Business rules

- Request prices must be greater than zero and monetary calculations use `BigDecimal`.
- Currency input is uppercased with `Locale.ROOT` and validated with `Currency.getInstance`; currencies are never summed together.
- Names are trimmed. Blank or null descriptions become null; nonblank descriptions are trimmed.
- Create defaults a missing status to `ACTIVE`.
- Update replaces all editable fields, but a missing status preserves the entity's current status.
- Cancel always sets status to `CANCELLED`; delete is a physical deletion.
- Every create assigns the current authenticated user on the server.
- Every list/get/update/cancel/delete/dashboard/upcoming query is restricted by current user id; a foreign UUID is returned as 404.
- Subscription lists are ordered by `createdAt` descending.
- Dashboard totals include only `ACTIVE` subscriptions.
- Yearly prices are divided by 12 for monthly totals; monthly prices are multiplied by 12 for yearly totals.
- Dashboard amounts are grouped by currency and rounded to scale 2 with `HALF_UP` after aggregation. Yearly-to-monthly conversion uses scale 8 before aggregation.
- Upcoming payments include active records from today through `today + days`, ordered by `nextPaymentDate` ascending. The `days` parameter must be between 1 and 365 and defaults to 30.

## API

Actual controller mappings:

```text
GET    /api/v1/subscriptions
GET    /api/v1/subscriptions/{id}
POST   /api/v1/subscriptions
PUT    /api/v1/subscriptions/{id}
PATCH  /api/v1/subscriptions/{id}/cancel
DELETE /api/v1/subscriptions/{id}
GET    /api/v1/dashboard?days=30
GET    /actuator/health
```

POST returns 201, DELETE returns 204, and the other successful application operations return 200. There is no PATCH for arbitrary edits and no API endpoint at `/`.

Web routes:

```text
GET  /login
POST /login                 # handled by Spring Security
GET  /signup
POST /signup
POST /logout                # handled by Spring Security
GET  /
GET  /subscriptions
GET  /subscriptions/new
POST /subscriptions
GET  /subscriptions/{id}/edit
POST /subscriptions/{id}
POST /subscriptions/{id}/cancel
POST /subscriptions/{id}/delete
GET  /admin
POST /admin/users/{id}/role
POST /admin/users/{id}/status
```

The web routes render Thymeleaf or redirect after mutations; they do not change the JSON API contract.

Public routes are `/login`, `/signup`, `/css/**`, `/js/**`, `/error`, and `/actuator/health`. All other routes require authentication, and `/admin/**` additionally requires `ADMIN`. Unauthenticated web requests redirect to `/login`; unauthenticated `/api/**` requests return the `ApiError` JSON shape with 401. CSRF is enabled. Ownership failures use 404 to avoid IDOR resource disclosure.

## Error handling

`GlobalExceptionHandler` is a `@RestControllerAdvice` that returns the `ApiError` record:

```text
timestamp, status, error, message, path, fieldErrors
```

- `SubscriptionNotFoundException` becomes 404.
- request-body Bean Validation failures become 400 with per-field errors;
- invalid ISO currency, controller parameter constraint violations, and unreadable JSON become 400;
- no custom catch-all 500 mapping currently exists.

## Logging

- Standard SLF4J/Logback is used; no additional logging framework or observability infrastructure is installed.
- Successful signup/login/logout and subscription create/update/cancel/delete operations log stable user/subscription UUID context.
- Duplicate signup, failed authentication, and rejected subscription access log at WARN without email, credentials, request bodies, session ids, cookies, CSRF tokens, or environment secrets.
- `AccountPrincipal.toString()` omits credentials and implements `CredentialsContainer` so Spring erases its password hash after authentication.
- Production keeps application logs at INFO and Spring Security/Hibernate SQL at WARN; do not enable verbose security or SQL logging by default.

## Database

- Database: PostgreSQL.
- Migration directory: `src/main/resources/db/migration/`.
- V1 creates `subscriptions`, enum-name checks, a positive-price check, the ISO-style uppercase currency check, and indexes on `status` and `(status, next_payment_date)`.
- V2 creates `users`, inserts a disabled non-login legacy owner, backfills existing subscriptions, makes `user_id` non-null with a foreign key, and adds ownership-aware indexes.
- JPA uses the default physical naming behavior plus explicit column names where needed.

Existing Flyway migrations may already have been applied in production.

Never modify, rename or delete an existing applied migration.

Create a new Flyway migration for every schema change.

Before any database change, inspect `src/main/resources/db/migration/`, the entity, repository queries, and relevant tests. Do not perform destructive schema or data operations without explicit user direction.

## Configuration

- `application.yml`: application name, `ddl-auto=validate`, disabled Open Session in View, Flyway enabled, optional `SERVER_PORT` defaulting to 8080, and Actuator `health`/`info` exposure.
- `application-local.yml`: local PostgreSQL defaults, all overridable.
- `application-prod.yml`: requires database values from the environment and provides no credential defaults.

Runtime environment-variable contract:

```text
SPRING_PROFILES_ACTIVE
DB_URL
DB_USERNAME
DB_PASSWORD
SERVER_PORT            # optional; defaults to 8080
POSTGRES_PASSWORD      # Compose PostgreSQL/local full-stack input
INITIAL_ADMIN_EMAIL    # optional one-time legacy-owner activation
INITIAL_ADMIN_PASSWORD # optional one-time legacy-owner activation; secret
INITIAL_ADMIN_DISPLAY_NAME # optional bootstrap display name
SESSION_COOKIE_SECURE  # optional; enable when production is HTTPS
```

Never place actual production values or secrets in source, logs, tests, documentation, or `AGENTS.md`.

The initial-admin variables must be supplied together and are only used while the disabled legacy owner exists. Activation replaces its unusable credential with a BCrypt hash and preserves ownership of production rows. It does not reset an already activated account. Remove the bootstrap secrets after successful activation. Sessions are stored in application memory; the current deployment assumes a single app instance.

## Testing

Existing tests are fast unit-style tests:

- `SubscriptionServiceTest`: creation defaults and input normalization;
- `DashboardServiceTest`: active-only, per-currency monthly/yearly calculations;
- `SubscriptionControllerTest`: valid/invalid POST, get, 404, and cancel using standalone MockMvc;
- `DashboardControllerTest`: default dashboard response using standalone MockMvc.
- `WebControllerTest`: rendered dashboard/list/form views and create/edit/cancel/delete flows using standalone MockMvc with Thymeleaf.
- `RegistrationServiceTest` and `AccountControllerTest`: normalization, BCrypt hashing, duplicates, signup validation, and password confirmation.
- `SecurityConfigTest`: web redirect, API 401, CSRF, successful login, wrong password, and unknown email.
- Service tests verify user-scoped list/read/write behavior and two-user dashboard isolation.

There are no repository integration, database, Compose, or Testcontainers tests. `SecurityConfigTest` is a focused Spring MVC/security application-context slice; the rest remain unit or standalone MockMvc tests.

Use the wrapper and JDK 21:

```bash
./mvnw test
./mvnw clean verify
```

On Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

If the machine's default Java is older than 21, point `JAVA_HOME` to a JDK 21 installation before running Maven.

## Required checks

Before finishing Java changes:

1. Run `./mvnw test` (or `.\mvnw.cmd test` on Windows).
2. Run `./mvnw clean verify` (or `.\mvnw.cmd clean verify`).
3. Inspect `git diff` and `git status`.

For Docker/config/runtime changes, additionally run where Docker is available:

```bash
docker build -t subscription-tracker .
docker compose --profile full up --build -d
curl http://localhost:8080/actuator/health
docker compose --profile full down
```

Do not add `-v` to `docker compose down`; preserve the named PostgreSQL volume unless deletion is explicitly requested. The Docker CLI was not available in the shell used to create this file, so re-run Docker checks in a Docker-enabled environment.

## Docker

- The root `Dockerfile` is a multi-stage build.
- Build image: `maven:3.9.11-eclipse-temurin-21`; it caches dependencies, copies `src`, and packages with tests skipped.
- Runtime image: `eclipse-temurin:21-jre-alpine`; it runs as non-root `spring:spring` and starts `/app/app.jar` with `java -jar`.
- Application port: 8080. The Dockerfile does not define a Docker `HEALTHCHECK` instruction.
- `compose.yaml` starts `postgres:17-alpine` with `pg_isready`, a named `subscription_tracker_data` volume, and host port 5432 for local development.
- The application Compose service is behind the `full` profile, depends on healthy PostgreSQL, publishes port 8080, and checks `/actuator/health` with `wget`.

```text
Spring Boot container
        -> Compose/Dokploy network
        -> PostgreSQL
        -> persistent database volume
```

## Production deployment

```text
Production deployment: EXISTS
Deployment platform: Dokploy
Hosting: VPS
Source repository: https://github.com/ilyakutarba-source/subscription-tracker
Deployment-sensitive branch: main
Containerization: Docker
```

Dokploy builds the repository-root `Dockerfile`, routes traffic to container port 8080, and supplies the `prod` profile/database environment variables. PostgreSQL is a separate persistent Dokploy service reachable from the application through the internal service network; it should not expose port 5432 publicly.

The deployment was performed manually from Dokploy's generic Git source. Do not assume every push automatically deploys; verify the configured provider/webhook and the actual deployment status in Dokploy.

## Existing deployment architecture

```text
Developer
  -> GitHub repository (`main`)
  -> Dokploy on VPS
  -> Dockerfile build
  -> Spring Boot container on 8080
  -> internal Docker/Dokploy network
  -> persistent PostgreSQL service
```

`/actuator/health` is the intended platform/runtime health endpoint. The application startup order is: connect to PostgreSQL, run/validate Flyway migrations, validate JPA mappings, then accept traffic.

Some production runtime configuration lives in Dokploy and is intentionally not stored in the repository. Verify those values directly in Dokploy when a task depends on them. Never record production passwords, webhook tokens, credentials, container IDs, or other secrets here.

The first deployment containing V2 requires a deliberate decision about legacy rows. To make them accessible, configure the one-time initial-admin variables in Dokploy before deployment, verify login and ownership, then remove the password variable. Port 8080, `/actuator/health`, the datasource contract, and the root Dockerfile remain unchanged.

## Deployment safety

1. Do not break or replace the existing Dockerfile without a task-specific reason.
2. Do not rename production environment variables without checking Dokploy impact.
3. Keep container/application port 8080 unless a coordinated deployment change is explicitly required.
4. Do not change the database connection contract without checking both Compose and Dokploy.
5. Do not remove or restrict `/actuator/health` without checking deployment health monitoring.
6. Never edit, rename, or delete an applied Flyway migration.
7. Create a new versioned migration for every schema change.
8. Do not execute destructive database changes without explicit user direction and a recovery plan.
9. Never commit secrets or real production credentials.
10. Treat startup, configuration, Docker, and migration changes as deployment regressions until verified.

## Production-sensitive files

Changes to these paths can affect local or production deployment:

```text
pom.xml
Dockerfile
compose.yaml
.dockerignore
.env.example
src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/application-prod.yml
src/main/resources/db/migration/**
```

For changes to these files, inspect the focused diff, run Maven verification, run relevant Docker checks where available, and preserve production migration/configuration compatibility.

## Git

- Current branch and deployment-sensitive branch: `main`.
- Remote: `origin` -> `https://github.com/ilyakutarba-source/subscription-tracker`.
- Recent milestones: persistence, CRUD/dashboard, tests, Docker/Compose, deployment documentation, and Spring Boot 4 Flyway auto-configuration.
- Changes pushed or merged to `main` may affect the Dokploy deployment, but automatic deploy-on-push is not guaranteed.

Before every task:

```bash
git status
git branch --show-current
```

For deployment or release work, also run:

```bash
git remote -v
git log --oneline -10
```

Do not use destructive Git commands unless the user explicitly asks for them. In particular, do not run `git reset --hard`, `git clean -fd`, or `git push --force`; do not delete others' commits, rewrite history, or modify unrelated files. Before finishing, inspect `git diff` and `git status`.

## Coding conventions

- Package root is `com.example.subscriptiontracker`; packages are organized by layer.
- Use constructor injection; there is no field injection and no Lombok.
- API DTOs are Java records named `*Request` and `*Response`.
- Keep entities internal and map responses explicitly through `SubscriptionMapper`.
- Use explicit domain exception names such as `SubscriptionNotFoundException` and centralize HTTP error translation.
- Services default to `@Transactional(readOnly = true)` and place `@Transactional` on writes.
- Repository methods use Spring Data derived-query naming.
- Resolve repository `Optional` values in services and raise a domain exception; do not expose `Optional` in the API.
- Tests use JUnit 5, Mockito, AssertJ, descriptive lower-camel-case test method names, and standalone MockMvc for controllers.
- Preserve current direct JSON response style unless the API contract explicitly requires a wrapper.

## Change rules

1. Read `AGENTS.md`, then open only the files relevant to the task.
2. Do not rescan the whole repository unless the relevant slice is insufficient.
3. Preserve the existing layered architecture and established contracts.
4. Make the smallest complete change and do not touch unrelated files.
5. Do not add dependencies without a concrete need.
6. Do not leave TODOs in place of requested implementation or add temporary debug endpoints.
7. Never store secrets or commit local `.env` files.
8. Preserve Docker/Dokploy compatibility.
9. Search for existing tests and analogous classes before adding duplicates.
10. Before changing an API, inspect its controller, DTOs, service, consumers documented in the repo, and tests.
11. Before changing schema, inspect all Flyway migrations, the entity/repository, and deployment implications.
12. Update tests with behavior changes and run the required checks.

## Scope protection

This is a focused learning project. Unless a separate task requires them, do not add Kafka, Redis, Kubernetes, microservices, WebFlux, CQRS, Event Sourcing, OAuth, AI integrations, or banking integrations. Prefer the simplest solution that fits the current Spring MVC/JPA architecture; do not overengineer.

## Important files

```text
pom.xml
README.md
Dockerfile
compose.yaml

src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/application-prod.yml
src/main/resources/db/migration/

src/main/java/com/example/subscriptiontracker/api/SubscriptionController.java
src/main/java/com/example/subscriptiontracker/api/DashboardController.java
src/main/java/com/example/subscriptiontracker/service/SubscriptionService.java
src/main/java/com/example/subscriptiontracker/service/DashboardService.java
src/main/java/com/example/subscriptiontracker/repository/SubscriptionRepository.java
src/main/java/com/example/subscriptiontracker/domain/Subscription.java
src/main/java/com/example/subscriptiontracker/domain/User.java
src/main/java/com/example/subscriptiontracker/exception/GlobalExceptionHandler.java
src/main/java/com/example/subscriptiontracker/security/SecurityConfig.java
src/main/java/com/example/subscriptiontracker/security/DatabaseUserDetailsService.java
src/main/java/com/example/subscriptiontracker/service/CurrentUserService.java
src/main/java/com/example/subscriptiontracker/service/RegistrationService.java
src/main/java/com/example/subscriptiontracker/web/WebController.java
src/main/java/com/example/subscriptiontracker/web/AccountController.java
src/main/resources/templates/
src/main/resources/static/css/app.css
src/test/java/com/example/subscriptiontracker/
```

## Repository-reading strategy

Do not scan the entire repository for every task.

Start from `AGENTS.md`.

Then locate the smallest relevant slice: Controller -> Service -> Repository -> related DTO/entity/tests.

For configuration tasks, inspect only the related config, Docker, Compose, and deployment files.

For database tasks, inspect the entity/repository plus all relevant Flyway migrations.

Expand the search only if the current slice is insufficient.

## Updating AGENTS.md

Update `AGENTS.md` only when project-level context changes: architecture, tech stack, API conventions, database strategy, production deployment, important environment variables, testing strategy, a major completed feature, or a major known issue. Do not update it for routine small fixes.
