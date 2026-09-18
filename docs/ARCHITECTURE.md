# RentManager — Architecture & API Guide

This document is a self-contained technical guide to the RentManager backend so that a frontend (or another AI) can be built against it without reading the source code. It covers the data model, business rules, authentication, every API endpoint with request/response examples, the error contract and the i18n contract.

Companion documents:

- `AGENTS.md` — project rules, roles, phases (source of truth for requirements)
- `docs/i18n.md` — translation conventions
- `database/README.md` — schema decisions

---

## 1. Stack and local setup

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security (JWT), Bean Validation, Maven wrapper |
| Database | MySQL 8.4 in Docker Compose (`docker-compose.yml`) |
| Current frontend (reference) | Angular 22 (standalone components, signals, zoneless), Transloco i18n, Vitest |

### Start everything

```bash
# 1. Database (Docker Desktop must be running). Applies database/01_schema.sql + 02_seed.sql on first run.
docker compose up -d          # MySQL on localhost:3306, database "rentmanager"

# 2. Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# 3. Frontend (http://localhost:4200)
cd frontend
npm install
npm start                     # ng serve, uses proxy.conf.json for /api → localhost:8080
```

Seed data (created by `database/02_seed.sql`):

- ADMIN user: `admin@rentmanager.local` / `admin1234` (seeded at backend startup unless `ADMIN_SEED_ENABLED=false`)
- Owner `Ana Propietaria` (id 1) with 2 properties: `Calle Mayor 12, 3ºA` Madrid 1200.00 AVAILABLE (id 1), `Avenida del Puerto 8` Valencia 950.00 MAINTENANCE (id 2)
- Tenant `Tomás Inquilino` (id 1) — no user account linked

Reset the database (re-applies schema + seed): `docker compose down -v && docker compose up -d`

### Tests and checks

```bash
cd backend && ./mvnw test                 # 69 integration/unit tests
cd frontend && npm test -- --watch=false  # 41 tests
cd frontend && npm run check:i18n         # es/en key parity (must pass)
```

---

## 2. System overview

```text
Browser (SPA)
   │  fetch /api/...  (Authorization: Bearer <JWT>)
   ▼
Angular dev server :4200  ──proxy /api──►  Spring Boot :8080  ──JDBC──►  MySQL :3306
```

- The SPA never talks to MySQL. All business rules are enforced by the backend.
- In development the Angular proxy avoids CORS. **Production CORS is not configured yet** — a frontend served from another origin must either proxy `/api` or CORS must be added to the backend.
- Schema is owned by `database/01_schema.sql`; Hibernate runs with `ddl-auto=validate` (it never alters tables).

Base URL for API calls: `/api` (relative). JSON only, UTF-8.

---

## 3. Data model

```text
users ──0..1──── owners ──1:N── properties ──1:N── contracts ──1:N── payments
  │                                  │                │
  │                                  └──1:N── maintenance_requests
  └──0..1──── tenants ───────────────┘ (tenant_id)
```

| Table | Key columns | Notes |
|---|---|---|
| `users` | id, email (unique), password_hash (BCrypt), full_name, role, active, created_at, updated_at | `role` is `ADMIN` / `OWNER` / `TENANT` (checked VARCHAR). There is **no roles table**. |
| `owners` | id, user_id (nullable, unique), full_name, email, phone, created_at | Business entity, can exist without login. `user_id` links to a `users` row with role OWNER. |
| `tenants` | id, user_id (nullable, unique), full_name, email, phone, created_at | Same, links to role TENANT. |
| `properties` | id, owner_id (FK), address, city, description, monthly_rent DECIMAL(10,2), status, created_at, updated_at | `monthly_rent > 0` enforced. |
| `contracts` | id, property_id (FK), tenant_id (FK), start_date, end_date, monthly_rent, status, created_at, updated_at | `start_date < end_date` enforced. The rent is copied from the property at creation time and never changes with the property. |
| `payments` | id, contract_id (FK), amount DECIMAL(10,2), due_date, paid_date (nullable), status, created_at, updated_at | `amount > 0`; unique `(contract_id, due_date)`. **Payments are never deleted.** |
| `maintenance_requests` | id, property_id (FK), created_by (FK users), title, description, status, assigned_to (nullable), created_at, updated_at | `created_by` is the authenticated user that created it. |

Database-level guarantees:

- At most **one ACTIVE contract per property** (unique generated column on `contracts`).
- One payment per contract and due date (prevents duplicate installments).
- Foreign keys prevent orphan records; deletions are protected in the service layer with specific error codes.

---

## 4. Business rules (must be respected by any client)

### 4.1 Property status

```text
AVAILABLE   → RENTED        (only via contract activation — never a manual action)
AVAILABLE   → MAINTENANCE
AVAILABLE   → INACTIVE
MAINTENANCE → AVAILABLE
INACTIVE    → AVAILABLE
RENTED      → AVAILABLE     (contract terminated or expired)
```

- Manual `→ RENTED` is rejected with `409 INVALID_STATE_TRANSITION`.
- `RENTED → MAINTENANCE/INACTIVE` is not allowed while a contract is active.
- `DELETE /api/properties/{id}` does not delete: it deactivates (`INACTIVE`). A `RENTED` property cannot be deactivated (`409`). Deactivation is ADMIN-only; owners can edit their own properties (data and status) but cannot reassign the owner, create or deactivate.

### 4.2 Contract lifecycle

```text
DRAFT → ACTIVE       (POST /activate)
ACTIVE → TERMINATED  (POST /terminate)
ACTIVE → EXPIRED     (lazy: evaluated on read when end_date < today)
```

- Activation validates: contract is DRAFT; no other ACTIVE contract on the property (`409 CONFLICT_ACTIVE_CONTRACT`); property status is AVAILABLE (`409 INVALID_STATE_TRANSITION`); `start_date < end_date` (`400 INVALID_DATE_RANGE`).
- Activation sets the property to `RENTED` and **generates one PENDING payment per month**: due the same day of each month from `start_date` while `due_date <= end_date`, amount = `contracts.monthly_rent`.
- Termination sets the property back to `AVAILABLE`. **Pending payments are not cancelled** (current decision; they can be cancelled individually).
- Expiration is evaluated lazily when contracts are listed/read: ACTIVE + `end_date < today` becomes EXPIRED and the property becomes AVAILABLE. There is no scheduler.
- Only DRAFT contracts can be edited (`PUT`) or deleted.

### 4.3 Payments

```text
PENDING → PAID        (records paid_date; defaults to today)
PENDING → OVERDUE     (lazy: evaluated on read when due_date < today and unpaid)
PENDING → CANCELLED
OVERDUE → PAID
OVERDUE → CANCELLED
```

- `PAID` and `CANCELLED` are final. Sending the same status again is a no-op (200).
- Only ADMIN can create payments manually or change payment status (tenants cannot self-register payments).
- There is no DELETE endpoint for payments.

### 4.4 Maintenance

```text
OPEN → IN_PROGRESS → COMPLETED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

- `COMPLETED` and `CANCELLED` are final.
- TENANT can create only for a property where they hold an **ACTIVE** contract; otherwise `403 FORBIDDEN`.
- Only ADMIN can update status and `assigned_to`.
- No DELETE endpoint.

### 4.5 Dashboard metric definitions

| Metric | Definition |
|---|---|
| `totalProperties` | all properties |
| `availableProperties` / `rentedProperties` | by status |
| `activeContracts` | status ACTIVE and `end_date >= today` |
| `pendingPayments` | status PENDING and `due_date >= today` |
| `overduePayments` | status OVERDUE + (PENDING and `due_date < today`) |
| `openMaintenanceRequests` | status OPEN only |
| `monthlyIncome` | sum of PAID payments whose `paid_date` is inside the current calendar month |

---

## 5. Authentication and authorization

### 5.1 JWT flow

1. `POST /api/auth/login` (or `/register`) returns `{ token, expiresAt, userId, email, fullName, role }`.
2. The client stores the token and sends it on every request: `Authorization: Bearer <token>`.
3. Tokens are HMAC-SHA256 JWTs with claims: `iss=rentmanager`, `sub=<email>`, `uid`, `role` (`ADMIN|OWNER|TENANT`), `name`, `iat`, `exp`. Default TTL: 480 minutes (`JWT_TTL_MINUTES`), secret from `JWT_SECRET` (≥32 bytes; dev default in `application.properties`).
4. On `401` for a protected endpoint the session is no longer valid: the client must clear its session and redirect to the login page. A `401` from `/api/auth/login` means bad credentials, **not** an expired session.
5. There is no refresh token: when the token expires the user logs in again.

### 5.2 Role model

- `ADMIN` — manages everything (properties, owners, tenants, contracts, payments, maintenance, users list, dashboard).
- `OWNER` — their own data: can edit their own properties (data and status, never reassign the owner) and read their contracts, payments and maintenance requests.
- `TENANT` — read-only over their own data: their contracts, their payments, their maintenance requests; can create maintenance requests for properties they rent.

### 5.3 Endpoint permission matrix

| Endpoint group | ADMIN | OWNER | TENANT | Anonymous |
|---|---|---|---|---|
| `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/health` | ✓ | ✓ | ✓ | ✓ |
| `GET /api/public/properties` | ✓ | ✓ | ✓ | ✓ (available properties only, no owner data) |
| `GET /api/users` | ✓ | — | — | — |
| `/api/owners/**`, `/api/tenants/**` (all methods) | ✓ | — | — | — |
| `GET /api/properties` | all | own only | — | — |
| `GET /api/properties/{id}` | any | own only (403 otherwise) | — | — |
| `POST`, `DELETE /api/properties/**` | ✓ | — | — | — |
| `PUT`, `PATCH /api/properties/**` | any | own only (data + status; owner cannot be reassigned) | — | — |
| `GET /api/contracts`, `GET /api/contracts/{id}` | all | own properties | own contracts | — |
| `POST/PUT/DELETE /api/contracts/**`, `POST .../activate`, `POST .../terminate` | ✓ | — | — | — |
| `GET /api/payments`, `GET /api/payments/{id}` | all | own properties | own contracts | — |
| `POST/PUT /api/payments/**` | ✓ | — | — | — |
| `GET /api/maintenance`, `GET /api/maintenance/{id}` | all | own properties | own created | — |
| `POST /api/maintenance` | any property | — | only rented property | — |
| `PUT /api/maintenance/{id}` | ✓ | — | — | — |
| `GET /api/dashboard` | ✓ | — | — | — |

Notes for the frontend:

- **`GET /api/properties` returns 403 for TENANT.** A tenant's rented property must be derived from their `ACTIVE` contracts (`GET /api/contracts` → `propertyId` / `propertyAddress`).
- Ownership is always verified server-side. Guards in the frontend are UX only.

---

## 6. API reference

All bodies are JSON. Errors follow the contract in section 7. `AuthResponse` shape:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-09-18T02:16:17.874373300Z",
  "userId": 1,
  "email": "admin@rentmanager.local",
  "fullName": "Administrator",
  "role": "ADMIN"
}
```

### 6.1 Authentication

| Method | Path | Auth | Body | Success |
|---|---|---|---|---|
| POST | `/api/auth/register` | public | `{ "email", "password" (8-72), "fullName" }` | `201` `AuthResponse` — **always creates a TENANT**; a `role` field in the body is ignored. `409 EMAIL_ALREADY_USED` |
| POST | `/api/auth/login` | public | `{ "email", "password" }` | `200` `AuthResponse`; `401 INVALID_CREDENTIALS` |
| GET | `/api/health` | public | — | `200 { "status": "UP" }` |

### 6.2 Users (ADMIN)

| Method | Path | Success |
|---|---|---|
| GET | `/api/users` | `200 [ { "id", "email", "fullName", "role", "active" } ]` |

There are no write endpoints for users yet (see section 10).

### 6.3 Owners (ADMIN)

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/api/owners` | — | `200 [ OwnerResponse ]` |
| GET | `/api/owners/{id}` | — | `200 OwnerResponse` |
| POST | `/api/owners` | `OwnerRequest` | `201 OwnerResponse` |
| PUT | `/api/owners/{id}` | `OwnerRequest` | `200 OwnerResponse` |
| DELETE | `/api/owners/{id}` | — | `204`; `409 OWNER_HAS_PROPERTIES` if the owner has properties |

```json
// OwnerRequest — userId is optional (null = unlink)
{ "fullName": "Ana Propietaria", "email": "ana@example.com", "phone": "+34 600 111 222", "userId": null }

// OwnerResponse
{ "id": 1, "fullName": "Ana Propietaria", "email": "ana@example.com", "phone": "+34 600 111 222", "userId": null, "createdAt": "2026-09-17T16:29:12Z" }
```

Link rules: the referenced user must exist (`404 USER_NOT_FOUND`), must have role `OWNER` (`400 USER_ROLE_MISMATCH`) and must not be linked to another owner (`409 USER_ALREADY_LINKED`).

### 6.4 Tenants (ADMIN)

Identical to owners, with role `TENANT` required for the link and:

- `DELETE /api/tenants/{id}` → `204`; `409 TENANT_HAS_CONTRACTS` if the tenant has contracts.
- `TenantRequest` / `TenantResponse` mirror `OwnerRequest` / `OwnerResponse`.

### 6.5 Properties

| Method | Path | Auth | Body | Success |
|---|---|---|---|---|
| GET | `/api/properties` | ADMIN (all), OWNER (own) | — | `200 [ PropertyResponse ]` |
| GET | `/api/properties/{id}` | ADMIN any, OWNER own | — | `200 PropertyResponse`; `403` if another owner's |
| POST | `/api/properties` | ADMIN | `PropertyRequest` | `201` (status is always `AVAILABLE`); `404 OWNER_NOT_FOUND` |
| PUT | `/api/properties/{id}` | ADMIN | `PropertyRequest` | `200` |
| PATCH | `/api/properties/{id}/status` | ADMIN | `{ "status": "MAINTENANCE" }` | `200`; `409 INVALID_STATE_TRANSITION` |
| DELETE | `/api/properties/{id}` | ADMIN | — | `204` (deactivates → `INACTIVE`); `409` if `RENTED` |

```json
// PropertyRequest  (description optional; monthlyRent > 0, 2 decimals, max 8 integer digits)
{ "ownerId": 1, "address": "Calle Mayor 12, 3ºA", "city": "Madrid", "description": null, "monthlyRent": 1200.00 }

// PropertyResponse
{ "id": 1, "ownerId": 1, "ownerName": "Ana Propietaria", "address": "Calle Mayor 12, 3ºA",
  "city": "Madrid", "description": null, "monthlyRent": 1200.00, "status": "AVAILABLE",
  "createdAt": "...", "updatedAt": "..." }
```

### 6.6 Contracts

| Method | Path | Auth | Body | Success |
|---|---|---|---|---|
| GET | `/api/contracts` | ADMIN all / OWNER own properties / TENANT own | — | `200 [ ContractResponse ]` (expiration is evaluated on read) |
| GET | `/api/contracts/{id}` | same rules | — | `200`; `403` if not yours |
| POST | `/api/contracts` | ADMIN | `ContractRequest` | `201` (`DRAFT`); `400 INVALID_DATE_RANGE`; `404 PROPERTY_NOT_FOUND` / `TENANT_NOT_FOUND` |
| PUT | `/api/contracts/{id}` | ADMIN | `ContractRequest` | `200` (DRAFT only; otherwise `409 INVALID_STATE_TRANSITION`) |
| POST | `/api/contracts/{id}/activate` | ADMIN | — | `200` ACTIVE; `409 CONFLICT_ACTIVE_CONTRACT` / `409 INVALID_STATE_TRANSITION` |
| POST | `/api/contracts/{id}/terminate` | ADMIN | — | `200` TERMINATED |
| DELETE | `/api/contracts/{id}` | ADMIN | — | `204` (DRAFT only) |

```json
// ContractRequest
{ "propertyId": 1, "tenantId": 1, "startDate": "2030-01-01", "endDate": "2030-12-31", "monthlyRent": 1200.00 }

// ContractResponse
{ "id": 34, "propertyId": 1, "propertyAddress": "Calle Mayor 12, 3ºA", "tenantId": 1,
  "tenantName": "Tomás Inquilino", "startDate": "2030-01-01", "endDate": "2030-12-31",
  "monthlyRent": 1200.00, "status": "ACTIVE", "createdAt": "...", "updatedAt": "..." }
```

### 6.7 Payments

| Method | Path | Auth | Body | Success |
|---|---|---|---|---|
| GET | `/api/payments` | ADMIN all / OWNER own properties / TENANT own | — | `200 [ PaymentResponse ]` (overdue evaluated on read) |
| GET | `/api/payments/{id}` | same rules | — | `200`; `403` if not yours |
| POST | `/api/payments` | ADMIN | `PaymentRequest` | `201` PENDING; `404 CONTRACT_NOT_FOUND`; `409 PAYMENT_ALREADY_EXISTS` |
| PUT | `/api/payments/{id}` | ADMIN | `PaymentUpdateRequest` | `200`; `409 INVALID_STATE_TRANSITION` |

```json
// PaymentRequest (manual payment)
{ "contractId": 34, "amount": 150.00, "dueDate": "2030-01-15" }

// PaymentUpdateRequest — paidDate optional, defaults to today when marking PAID
{ "status": "PAID", "paidDate": "2030-01-20" }

// PaymentResponse
{ "id": 161, "contractId": 34, "propertyId": 1, "propertyAddress": "Calle Mayor 12, 3ºA",
  "tenantName": "Tomás Inquilino", "amount": 1200.00, "dueDate": "2030-01-01",
  "paidDate": null, "status": "PENDING", "createdAt": "...", "updatedAt": "..." }
```

### 6.8 Maintenance

| Method | Path | Auth | Body | Success |
|---|---|---|---|---|
| GET | `/api/maintenance` | ADMIN all / OWNER own properties / TENANT own created | — | `200 [ MaintenanceResponse ]` |
| GET | `/api/maintenance/{id}` | same rules | — | `200`; `403` if not yours |
| POST | `/api/maintenance` | ADMIN (any property) / TENANT (active contract only) | `{ "propertyId", "title", "description" }` | `201` OPEN; `403` for TENANT without active contract; `404 PROPERTY_NOT_FOUND` |
| PUT | `/api/maintenance/{id}` | ADMIN | `{ "status": "IN_PROGRESS", "assignedTo": "Fontanero Pérez" }` | `200`; `409 INVALID_STATE_TRANSITION`. `assignedTo: null` leaves the current value unchanged |

```json
// MaintenanceResponse
{ "id": 10, "propertyId": 1, "propertyAddress": "Calle Mayor 12, 3ºA", "createdById": 517,
  "createdByName": "Runtime Maint", "title": "Fuga en el baño", "description": "Gotea el grifo",
  "status": "OPEN", "assignedTo": null, "createdAt": "...", "updatedAt": "..." }
```

### 6.9 Dashboard (ADMIN)

```http
GET /api/dashboard
```

```json
{ "totalProperties": 2, "availableProperties": 1, "rentedProperties": 0, "activeContracts": 0,
  "pendingPayments": 0, "overduePayments": 0, "openMaintenanceRequests": 0, "monthlyIncome": 0 }
```

### 6.10 Public properties (anonymous)

Used by the public landing, search and detail pages. Only properties in `AVAILABLE` status are exposed, and **no owner or tenant data is returned**.

| Method | Path | Body | Success |
|---|---|---|---|
| GET | `/api/public/properties?query=` | — | `200 [ PublicPropertyResponse ]`; `query` (optional) matches city or address, case-insensitive; ordered by rent ascending |
| GET | `/api/public/properties/{id}` | — | `200 PublicPropertyResponse`; `404 PROPERTY_NOT_FOUND` when it does not exist **or is not available** |

```json
{ "id": 1, "address": "Calle Mayor 12, 3ºA", "city": "Madrid", "description": null,
  "monthlyRent": 1200.00, "status": "AVAILABLE" }
```

---

## 7. Error contract

Every error response is language-neutral and follows this shape (the `errors` array is omitted when empty):

```json
{ "code": "PROPERTY_NOT_FOUND" }
```

```json
{ "code": "VALIDATION_ERROR", "errors": [ { "field": "monthlyRent", "code": "POSITIVE" } ] }
```

| HTTP | Codes |
|---|---|
| 400 | `VALIDATION_ERROR`, `BAD_REQUEST` (malformed JSON, wrong parameter type), `INVALID_DATE_RANGE`, `USER_ROLE_MISMATCH` |
| 401 | `UNAUTHORIZED` (missing/invalid/expired token), `INVALID_CREDENTIALS` |
| 403 | `FORBIDDEN` |
| 404 | `NOT_FOUND`, `USER_NOT_FOUND`, `OWNER_NOT_FOUND`, `TENANT_NOT_FOUND`, `PROPERTY_NOT_FOUND`, `CONTRACT_NOT_FOUND`, `PAYMENT_NOT_FOUND`, `MAINTENANCE_NOT_FOUND` |
| 405 | `METHOD_NOT_ALLOWED` |
| 409 | `EMAIL_ALREADY_USED`, `USER_ALREADY_LINKED`, `OWNER_HAS_PROPERTIES`, `TENANT_HAS_CONTRACTS`, `PAYMENT_ALREADY_EXISTS`, `CONFLICT_ACTIVE_CONTRACT`, `INVALID_STATE_TRANSITION`, `CONFLICT` (generic DB constraint fallback) |
| 500 | `INTERNAL_ERROR` |

Field validation codes (inside `errors[]`): `REQUIRED`, `EMAIL`, `POSITIVE`, `POSITIVE_OR_ZERO`, `SIZE`, `PATTERN`, `MIN`, `MAX`, `FUTURE`, `PAST`, `INVALID`.

The frontend must translate codes, never display them raw. Unknown codes fall back to a generic "unexpected error" message.

---

## 8. Internationalisation contract

- All user-facing text comes from translation keys; nothing hardcoded. Current app supports `es` and `en` with runtime switching.
- Namespaces: `common.*`, `nav.*`, `auth.*`, `home.*`, `dashboard.*`, `properties.*`, `owners.*`, `tenants.*`, `contracts.*`, `payments.*`, `maintenance.*`, `notFound.*`, `enums.*`, `validation.*`, `errors.*`.
- Business values are **language-neutral codes**: `status` fields are always `AVAILABLE`, `RENTED`, `DRAFT`, `PENDING`, etc. Translate the label only:
  `{{ 'enums.propertyStatus.' + property.status | transloco }}`
- Error translation: `errors.<CODE>` (e.g. `errors.PROPERTY_NOT_FOUND`), fallback `errors.UNKNOWN`.
- Validation translation: `validation.<code in lowercase>` (e.g. `validation.required`, `validation.minLength` with `{ min }` param).
- Dates, numbers and currency must be formatted with the active locale (Angular `date`/`number` pipes with the locale passed as argument; the backend sends ISO strings `yyyy-MM-dd` for dates and plain JSON numbers for money).
- Key parity between `es.json` and `en.json` is mandatory and checked by `npm run check:i18n`.
- A language selector must persist the choice (current reference uses `localStorage` key `rentmanager.lang`) and update `document.documentElement.lang`.

---

## 9. Reference frontend (current implementation)

Project: `frontend/` — Angular 22, standalone components, signals, zoneless, Transloco, Vitest.

```text
src/app/
├── core/
│   ├── guards/            authGuard (redirects to /login?returnUrl=...), roleGuard(...roles)
│   ├── i18n/              language.ts, language.service.ts, transloco-loader.ts, translated-title.strategy.ts
│   ├── interceptors/      auth.interceptor.ts (adds Bearer token; 401 on protected endpoints → logout)
│   ├── seo/               robots.service.ts (noindex by default, `data: { public: true }` opts in)
│   └── services/          auth, api-error, property, owner, tenant, user, contract, payment, maintenance, dashboard
├── shared/
│   ├── components/        language-selector
│   ├── forms/             fieldErrorMessage() helper
│   └── models/            typed models for every API resource
├── features/              auth (login/register), home, dashboard, properties, owners, tenants, contracts,
│                          payments, maintenance, not-found  (each lazy-loaded)
└── app.routes.ts          routes with title keys + guards
```

Conventions worth keeping in any new frontend:

- One service per API resource in `core/services`, typed models in `shared/models`; components never call `HttpClient` directly.
- Route titles are translation keys and a custom `TitleStrategy` re-translates them when the language changes.
- Session storage keys: `rentmanager.token` (JWT), `rentmanager.auth` (user JSON with `expiresAt`). An expired stored session is discarded on load.
- Dev proxy: `proxy.conf.json` maps `/api` → `http://localhost:8080`.
- Private routes must not be indexable (meta `robots: noindex`); public pages (home, property search, property details — none exist yet) must be marked `data: { public: true }`. Do not add SSR/prerender/`llms.txt` without an explicit decision.
- Accessibility basics: semantic HTML, one `h1` per page, labels tied to inputs, keyboard operable, sufficient contrast, meaningful `alt` text, favicon.

Useful flows for manual verification:

1. Login as admin → Dashboard shows seed metrics.
2. Create owner → create property for that owner → create contract (DRAFT) → activate → property becomes RENTED and installments appear in Payments → mark one PAID → `monthlyIncome` updates.
3. Register a new user → tenant profile → admin links the user to a tenant → create+activate a contract for them → log in as that tenant: they see only their contract/payments and can create a maintenance request for the rented property.

---

## 10. Known gaps and pending decisions

- **No user management write API**: admins can only list users. Owner/admin accounts cannot be created through the API (register only creates TENANT). Manual testing of OWNER roles requires promoting a registered user in the database:
  `UPDATE users SET role='OWNER' WHERE email='...';` then log in again (the role lives inside the JWT).
- Terminating a contract does **not** cancel its pending payments (they can be cancelled individually).
- `monthlyIncome` means collected income this month (not expected rent from active contracts).
- No refresh tokens, no password reset, no pagination/filtering on list endpoints (small data volume assumed).
- Production CORS is not configured (dev relies on the Angular proxy).
- Public pages (home, property search, property details) do not exist yet; SEO assets (meta descriptions, Open Graph, canonical URLs, sitemap) are intentionally deferred until they exist.
