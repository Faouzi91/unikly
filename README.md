# Unikly

Unikly is an e-commerce marketplace foundation with buyer and seller accounts, profile management, a persistent product catalog and cart, stock-aware order placement, buyer order history, seller fulfillment updates, and product reviews. Payment collection and carrier tracking are not integrated. Current requirements are in [docs/UNIKLY_SRS_v4.md](docs/UNIKLY_SRS_v4.md); the phased implementation sequence and shared progress are in [docs/product-roadmap.md](docs/product-roadmap.md). The v2/v3 SRS files and onboarding guide are historical snapshots, including the superseded freelance-services direction.

## Current authentication slice

- Email and password registration, login, logout, and password change.
- Registration accounts remain disabled until the user confirms the time-limited verification link. Password recovery uses a single-use link and ends active sessions after a successful reset.
- The local Compose stack captures verification and recovery email in Mailpit at <http://localhost:8025>; this sink is for development only. Production email delivery and shared rate limiting still require deployment configuration and operational review.
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

Open <http://localhost:4200>. Browse/search products, add items to the basket, create a buyer or seller account, and confirm the message captured in Mailpit at <http://localhost:8025> before signing in. Recovery messages are captured there too. The demo checkout does not collect payment. PostgreSQL is exposed only on localhost at port 5432 by default. If that port is already in use, set `DATABASE_HOST_PORT` (for example, to `5433`) in `.env`. The API is exposed on port 8080 by default; set `API_HOST_PORT` to another free port if needed. `.env` is ignored by Git; never commit it or use its local values in a public deployment.

The development account seeder is disabled by default. To enable it, set `UNIKLY_SEED_DEV_USERS=true` and provide non-empty, local-only values for `DEV_CUSTOMER_PASSWORD`, `DEV_ADMIN_PASSWORD`, and `DEV_SELLER_PASSWORD` in `.env`. The account emails can also be changed there. Configured accounts are reset to their configured roles and passwords each time the development API starts. Do not publish `.env` or enable development seeding on a public deployment.


Stop the services with `docker compose down`. The database volume is retained; `docker compose down -v` deletes local database data.

## Development and tests

Use the pinned toolchains through Docker. Backend tests use isolated in-memory H2 and do not connect to the local PostgreSQL service:

    docker run --rm -v "$PWD/backend:/workspace" -w /workspace maven:3.9.16-eclipse-temurin-25 mvn -B -ntp test

Build the Angular production image with the Node version pinned in the frontend Dockerfile:

    docker build -t unikly-web-test ./frontend

Frontend tests use Karma and require Chromium. Run them in a container with the pinned Node version and a compatible browser; the production image build does not run unit tests. See [the Docker toolchain skill](.agents/skills/docker-toolchain-isolated/SKILL.md) for the current isolated workflows.

GitHub Actions runs backend Maven verification, including module-boundary rules, and the Angular production build on pushes and pull requests. Use the [implementation roadmap](docs/product-roadmap.md) for current product sequencing, [release readiness](docs/release-readiness.md) for open launch gates, and the pull request template for issue links, acceptance criteria, and verification evidence.

For Angular dev server work, use Node 24.21.0 from .nvmrc (or a Node container), start PostgreSQL/API with Compose or locally, then run npm start inside frontend. The dev server proxies /api to 127.0.0.1 using API_HOST_PORT from the root .env file, defaulting to port 8080.

## AI coding assistants

Project-wide instructions are maintained in [AGENTS.md](AGENTS.md). Gemini CLI loads [GEMINI.md](GEMINI.md), which imports those rules; Gemini CLI also discovers the reusable project skills in .agents/skills/.

With Gemini CLI installed and authenticated, start it from the repository root and trust the workspace if prompted. Check skill discovery with gemini skills list or /skills list in an interactive session, and check loaded instructions with /memory show. If skills were added during the current session, run /skills reload or restart. No separate workspace extension or global skill installation is required.

### Gemini delivery agents

Project reviewers are defined in .gemini/agents/ and limited to read-only file/search tools. The primary session coordinates the relevant reviewers and owns all edits and final verification. Use /deliver to plan and implement a shippable slice; use /review to audit current changes without editing. Check /agents list and /commands list in Gemini CLI. No model or global settings need to be changed; reviewers inherit the selected session model. See [the ECC skill mapping and agent workflow](docs/ai-agent-workflow.md) for the curated skills, specialist roles, and delivery gates.

## API endpoints

- `GET /api/auth/csrf`: initialize/read the CSRF token before a state-changing request.
- `POST /api/auth/register`: `{ "email", "password", "displayName", "accountType": "BUYER" | "SELLER" }`; Admin is never assignable through public registration.
- `POST /api/auth/verification/request` and `/api/auth/verification/confirm`: request a verification message or confirm its single-use token.
- `POST /api/auth/password-recovery/request` and `/api/auth/password-recovery/confirm`: request a reset message or set a new password with its single-use token.
- `POST /api/auth/login`: `{ "email", "password" }`.
- `POST /api/auth/logout`: invalidates the current session. Include the `X-XSRF-TOKEN` header.
- `GET /api/auth/me`: current authenticated account.
- `POST /api/auth/password`: authenticated password change; requires the current password and CSRF token.
- `GET /api/profile/me`: current customer's profile.
- `PUT /api/profile/me`: update current customer's profile and default delivery address.
- `GET /api/reviews/summary`: public aggregate product ratings for catalog listings.
- `GET /`: storefront landing page.
- `GET /api/admin/access-check`: example admin-only route.
- `GET /actuator/health`: health probe.

For production deployment, terminate TLS, set `SESSION_COOKIE_SECURE=true`, replace local database credentials with managed secrets, use a production email sender and shared rate limiter, and review security settings before opening registration to the public. The current in-memory request limiter and Mailpit sink are single-instance development implementations.
