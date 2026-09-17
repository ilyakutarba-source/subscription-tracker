# Product validation and implementation plan

## 1. Product validation

- **Primary user:** one person who manually tracks personal recurring paid services.
- **Problem:** small recurring payments are scattered, so the user loses visibility into total spend and the next charge dates.
- **Jobs to Be Done:** record a subscription once; see what is active; understand monthly/yearly spend by currency; notice charges due soon; cancel or remove obsolete records.
- **MVP outcome:** the user can maintain a trustworthy subscription list and answer “what will I pay, in which currency, and when?” without a spreadsheet.

## 2. MVP boundary

Included: create/read/update/delete, explicit cancellation, `ACTIVE`/`PAUSED`/`CANCELLED`, monthly/yearly billing, category, dashboard totals per currency, sorted upcoming payments for a configurable 1–365 day window, validation, consistent errors and health endpoint.

Excluded deliberately: accounts, authentication, notifications, price history, custom categories, exchange rates, bank import, recurring-payment detection, payment execution, Kafka, Redis, WebFlux, CQRS, Kubernetes and AI.

## Assumptions and constraints

- MVP is single-user and intentionally has no owner column.
- The caller advances `nextPaymentDate`; automatic renewal scheduling is not part of MVP.
- Currency is a valid three-letter ISO 4217 code. No exchange-rate conversion occurs.
- A cancelled or paused subscription is excluded from dashboard costs and upcoming payments.
- Dates are calendar dates; audit timestamps are UTC instants.
- Local and production databases are PostgreSQL. Flyway is the schema authority and Hibernate only validates.

## 3. Technical specification

### Domain

`Subscription`: UUID id, name, description, decimal price, currency, billing period, start/next-payment dates, category, status and audit timestamps. Enums are stored by name.

### Components

- `SubscriptionController`: HTTP CRUD/cancel and DTO validation.
- `DashboardController`: dashboard HTTP endpoint and payment-window validation.
- `SubscriptionService`: lifecycle operations, normalization and lookup.
- `DashboardService`: active-only, per-currency cost calculation and upcoming payments.
- `SubscriptionRepository`: JPA persistence and narrow queries.
- `SubscriptionMapper`: entity-to-response boundary.
- `GlobalExceptionHandler`: one error contract including field errors.

### Data path

```text
HTTP -> Controller -> validated DTO -> Service -> Repository -> PostgreSQL
                                                    |
HTTP <- response DTO <- Mapper <--------------------+
```

### Database

Flyway `V1__create_subscriptions.sql` creates `subscriptions`, positive-price and enum-like checks, plus indexes on `status` and `(status, next_payment_date)`.

## 4. Project structure

```text
com.example.subscriptiontracker
├── api                 controllers, mapper
│   └── dto             request/response contracts
├── domain              entity and enums
├── exception           API error handling
├── repository          persistence ports
└── service             business use cases
```

The structure is layer-oriented because the MVP has one small aggregate. Feature modules would add ceremony without isolation benefits yet.

## 5. Task decomposition

| # | Task and goal | Main files | Acceptance criteria and tests |
|---|---|---|---|
| 1 | Bootstrap Java 21/Spring Boot project | `pom.xml`, app class, wrapper | Maven project resolves and compiles |
| 2 | Define product/technical contract | this document | MVP, exclusions, assumptions, entities and flow are explicit |
| 3 | Add PostgreSQL schema | application YAML, `V1...sql`, entity, repository | Flyway owns schema; JPA uses `validate`; constraints/indexes exist |
| 4 | Implement CRUD and cancellation | DTOs, mapper, controller, service | all required endpoints return DTOs; missing id is 404; cancel sets `CANCELLED` |
| 5 | Implement dashboard | dashboard controller/service/DTO | active-only totals; yearly/12; monthly*12; currencies separate; dates sorted |
| 6 | Add errors and validation | request DTO, advice, `ApiError` | invalid body is 400 with field errors; malformed requests use common format |
| 7 | Add automated tests | `src/test` | create, conversions, cancelled exclusion, currencies, API validation/404/cancel/dashboard |
| 8 | Add container deployment | Dockerfile, Compose, profiles | multi-stage image; non-root runtime; persistent PostgreSQL; health checks |
| 9 | Document operations and Dokploy | `README.md`, `.env.example` | local, full Docker, API, env and deployment steps are reproducible |
| 10 | Git/GitHub delivery | `.gitignore`, commits, remote | logical commits; no secrets; `main` pushed when GitHub tooling is available |

Each task is complete only after its relevant tests/compile check and a clean review of `git diff`.

## Roadmap after MVP

- **V2:** users, Spring Security/JWT, ownership, search/filter/sort, payment reminders.
- **V3:** Telegram/email delivery, price history, analytics/charts, custom categories.
- **V4:** bank transaction import, recurring-payment detection and exchange-rate API.

