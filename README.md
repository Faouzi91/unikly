# Unikly

Unikly is an e-commerce product store with buyer and seller accounts, profile management, a persistent product catalog, stock-aware checkout, buyer order history, and seller fulfillment updates for their own products. Payment collection and carrier tracking are not integrated. The active product-store requirements are in [UNIKLY_SRS_v3.docx](UNIKLY_SRS_v3.docx). The preserved [UNIKLY_SRS_v2.docx](UNIKLY_SRS_v2.docx) and onboarding guide document the superseded freelance services marketplace direction.

## Current authentication slice

- Email and password registration, login, logout, and password change.
- Public registration lets people choose Buyer or Seller accounts; the API accepts only those two choices and never allows a public request to assign Admin.
- Argon2 password hashes; the application never stores plaintext passwords.
- Server-side sessions in HttpOnly, SameSite=Strict cookies; no bearer tokens in browser storage.
- CSRF protection on state-changing requests, session ID rotation on login, and server-side permission checks.
- `/api/auth/me` requires `ACCOUNT_READ_SELF`. `/api/admin/access-check` requires `PLATFORM_REPORT_READ`. Unmatched `/api/**` routes are denied by default.
- `/api/profile/me` supports reading and updating the authenticated customer's display name, phone, and default delivery address. The customer ID is always taken from the authenticated session.
- `POST /api/auth/password` verifies the current password, hashes the new one, and invalidates active sessions.
- The account screen edits profile/security data and shows the permission set granted by the server for the signed-in role.
- The Compose dev profile can seed a local admin only when UNIKLY_SEED_DEV_USERS=true. This opt-in seeder never runs outside the dev profile. Production admins must still be provisioned through a controlled, access-managed procedure; never add a public role-change endpoint or expose admin credentials in the UI.

See [docs/authorization-matrix.md](docs/authorization-matrix.md) for the role-to-permission matrix, permission meanings, and rules for adding protected API endpoints.

The application structure and module dependency rules are in [docs/architecture.md](docs/architecture.md). The backend is a package-by-capability modular monolith; the Angular app uses a small composition shell and lazy feature routes.

## Run locally

Requirements: Docker with Compose. Use Java 25.0.4 (`.java-version`) and Node 24.21.0 (`.nvmrc`). Toolchain versions are pinned in the Maven Wrapper, Maven/Temurin backend image, Angular lockfile, and Node/nginx frontend images. The current stack targets Spring Boot 4.1, Maven 3.9.16, Angular 22.2, PostgreSQL 18.6, and nginx 1.30.5. Dependabot checks Maven, npm, and container dependencies weekly.

```sh
cp .env.example .env
# Edit .env and set a local-only DATABASE_PASSWORD before starting.
docker compose up --build
```

Open <http://localhost:4200>. Browse/search products, add items to the basket, create a buyer or seller account, place an order, and manage profile/security settings. PostgreSQL is exposed only on localhost at port 5432 by default. If that port is already in use, set `DATABASE_HOST_PORT` (for example, to `5433`) in `.env`. The API is exposed on port 8080 by default; set `API_HOST_PORT` to another free port if needed. `.env` is ignored by Git; never commit it or use its local values in a public deployment.

The development account seeder is disabled by default. To enable it, set `UNIKLY_SEED_DEV_USERS=true` and provide non-empty, local-only values for `DEV_CUSTOMER_PASSWORD`, `DEV_ADMIN_PASSWORD`, and `DEV_SELLER_PASSWORD` in `.env`. The account emails can also be changed there. Configured accounts are reset to their configured roles and passwords each time the development API starts. Do not publish `.env` or enable development seeding on a public deployment.


Stop the services with `docker compose down`. The database volume is retained; `docker compose down -v` deletes local database data.

## Development and tests

Backend tests run with the Maven Wrapper and use isolated in-memory H2, so they do not connect to your local PostgreSQL service:

```sh
cd backend
./mvnw test
```

Frontend build:

```sh
cd frontend
npm ci
npm run build
```

Frontend tests compile and run with Karma. A Chrome/Chromium installation is required for the browser execution phase.

For Angular dev server work, start PostgreSQL/API with Compose or locally, then run `npm start` inside `frontend`; Compose exposes the API on `127.0.0.1:8080` and the dev server proxies `/api` there.

## API endpoints

- `GET /api/auth/csrf`: initialize/read the CSRF token before a state-changing request.
- `POST /api/auth/register`: `{ "email", "password", "displayName", "accountType": "BUYER" | "SELLER" }`; Admin is never assignable through public registration.
- `POST /api/auth/login`: `{ "email", "password" }`.
- `POST /api/auth/logout`: invalidates the current session. Include the `X-XSRF-TOKEN` header.
- `GET /api/auth/me`: current authenticated account.
- `POST /api/auth/password`: authenticated password change; requires the current password and CSRF token.
- `GET /api/profile/me`: current customer's profile.
- `PUT /api/profile/me`: update current customer's profile and default delivery address.
- `GET /`: storefront landing page.
- `GET /api/admin/access-check`: example admin-only route.
- `GET /actuator/health`: health probe.

For production deployment, terminate TLS, set `SESSION_COOKIE_SECURE=true`, replace local database credentials with managed secrets, configure rate limiting and email-based verification/password recovery, and review security settings before opening registration to the public.