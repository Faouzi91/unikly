# Unikly project instructions

These rules apply to contributors and coding agents working in this repository.

## Product scope and sources of truth

- Unikly is currently an e-commerce storefront with buyer and seller accounts, a product catalog, cart, checkout and orders, fulfillment, profiles, and product reviews. Payment collection and carrier integrations are not implemented.
- Use [docs/UNIKLY_SRS_v4.md](docs/UNIKLY_SRS_v4.md) for active product requirements and status, [docs/product-roadmap.md](docs/product-roadmap.md) for sequencing and shared progress, [README.md](README.md) for run instructions, and [docs/architecture.md](docs/architecture.md) plus [docs/authorization-matrix.md](docs/authorization-matrix.md) for implementation boundaries.
- [UNIKLY_SRS_v3.docx](UNIKLY_SRS_v3.docx), [UNIKLY_SRS_v2.docx](UNIKLY_SRS_v2.docx), and [UNIKLY_BUILD_AND_ONBOARDING_GUIDE.md](UNIKLY_BUILD_AND_ONBOARDING_GUIDE.md) are historical snapshots. Do not use them to define current product work.
- Ship a small user-visible slice at a time. Keep the backend a modular monolith. Do not add payment gateways, carrier integrations, microservices, or other deferred infrastructure unless current requirements explicitly call for them.
- If repository code and a rule disagree, inspect the current implementation and update the stale rule or documentation as part of the task.

## Delivery workflow

1. Inspect the relevant source, docs, tests, and Git status before editing. Preserve unrelated user changes and untracked files.
2. For multi-file or behavior-changing work, state the intended result and acceptance checks before implementation. Resolve routine choices from current code and requirements.
3. Make the smallest complete change. Reuse existing patterns and avoid unrelated refactors or dependencies.
4. Review the final diff as a reviewer: check behavior, authorization, data ownership, migrations, accessibility, responsive UI, secrets, and accidental scope.
5. Verify the affected layer using Docker workflows in [the toolchain skill](.agents/skills/docker-toolchain-isolated/SKILL.md). Run a local Compose smoke test for changes that affect a real user flow, authorization, persistence, or service wiring. Documentation-only changes do not require application builds or live-stack runs.
6. Report commands actually run and their results. If a relevant check cannot run, say why; do not claim unverified work passed.
7. Add a dated, domain-categorized entry to [CHANGELOG.md](CHANGELOG.md) for each feature, bug fix, or maintained project-rule change.

## Git workflow

- Keep non-trivial work on a focused topic branch; use feat/, fix/, refactor/, docs/, or chore/ prefixes. Do not commit directly to master.
- Inspect and preserve existing work before switching branches. Do not reset, clean, amend shared commits, force-push, merge, or push unless the user requests that action.
- Use a Conventional Commit message if a commit is requested. Keep changes scoped and reviewable.

## Toolchains and local services

- Project versions are pinned in .java-version, .nvmrc, Maven configuration, lockfiles, and Dockerfiles. Current targets are Java 25.0.4, Node 24.21.0, Spring Boot 4.1, Angular 22.2, and PostgreSQL 18.6.
- Use Docker for Maven tests and Angular production builds; do not rely on whatever Java or Node version happens to be installed on the host. See [the isolated toolchain skill](.agents/skills/docker-toolchain-isolated/SKILL.md) for commands.
- Use docker compose for local full-stack work. Keep local .env values private. Do not print or copy secrets into logs, prompts, docs, or commits.
- Never run docker compose down -v as routine cleanup; it deletes the local database volume.

## Architecture and placement

- Backend source lives under backend/src/main/java/com/unikly/store/. Current modules are identity, catalog, cart, orders, profile, reviews, and cross-cutting platform.
- Keep each module's domain, application, persistence, and API code inside that module. Add only layers a feature needs; do not create empty modules or placeholder packages.
- Cross-module business operations go through the owning module's public application service. Do not reach into another module's repositories or mutate its entities directly.
- Database migrations live in backend/src/main/resources/db/migration/. Never edit or delete an applied migration; add a forward migration.
- Angular pages live in frontend/src/app/features/; shared state and API clients live in frontend/src/app/core/. Keep feature routes and internals behind their public entry points.
- `app.routes.ts` composes only route exports from feature `public-api.ts` files. Keep storefront, catalog, cart/checkout, and seller workflows in their owning features; shared catalog API/state belongs in `core/catalog/`.
- For exact file placement and current structure, use [the project structure skill](.agents/skills/project-structure-navigator/SKILL.md).

## Backend rules

- Use immutable Java records for request and response DTOs and validate untrusted input at the API boundary.
- Keep transaction boundaries in application services. Use read-only transactions for query operations where appropriate.
- Enforce authorization on the server and check resource ownership in the application service. UI guards and hidden controls are not security boundaries.
- Keep API access deny-by-default. Add the narrow permission needed for a real operation and update [the authorization matrix](docs/authorization-matrix.md).
- Use BigDecimal for money. The server computes prices, delivery fees, and order totals; never trust client totals.
- Keep exception handling scoped to the responsible API/module and return no secrets, SQL details, or stack traces to clients.

## Frontend rules

- Use standalone Angular components and inject() for dependency injection. Prefer Signals for local and shared reactive state where they fit.
- Use Angular control flow (@if, @for with a stable track, and @switch); do not introduce NgModules.
- Keep templates semantic and accessible. Label controls, provide useful keyboard/focus behavior, and expose dynamic status updates accessibly.
- Use the existing Lucide Angular icon package for interface icons; do not use emoji icons or add verbose inline SVGs.
- Follow existing image loading and fallback patterns. Do not apply external-image rules to local assets without checking how the asset is served.

## Database and security

- Use forward-only Flyway migrations with appropriate constraints, indexes, and ownership relationships.
- Keep credentials and keys out of source control. .env.example may contain safe placeholders only.
- For authentication, authorization, CSRF, ownership, or sensitive data changes, read [the security skill](.agents/skills/web-security-owasp/SKILL.md) and the authorization matrix.

## Gemini CLI and shared skills

- Gemini CLI should load the root GEMINI.md, which imports these project instructions.
- Reusable project skills live in .agents/skills/. Keep each SKILL.md frontmatter with a concise name and task-specific description; avoid copying large generic rule sets.
- Use the [feature delivery skill](.agents/skills/feature-delivery/SKILL.md) for feature, bug-fix, and refactor work. It adapts ECC-style planning, review, and verification to Unikly's current product and toolchains.
- Track open work with GitHub issues and progress/check evidence with the pull request template. Keep [docs/product-roadmap.md](docs/product-roadmap.md) as the shared product sequence/status, [docs/release-readiness.md](docs/release-readiness.md) as the launch-gate checklist, and CHANGELOG.md as completed-work history.
- Record repository documentation paths as relative paths. Do not add machine-specific paths or user-specific secrets.

## Gemini specialist agents and orchestration

- The primary Gemini session is the delivery orchestrator and remains responsible for planning, implementation, integrating reviewer feedback, verification, and the final report. Use .agents/skills/feature-delivery/SKILL.md for multi-file or behavior-changing work.
- Codex, Gemini, and other contributors use the same task context: read the active SRS and roadmap, then the linked GitHub issue/PR, architecture, and authorization matrix as relevant. The primary orchestrator gives every delegated reviewer the same requirement IDs, roadmap increment, acceptance criteria, changed paths, and focused question; refresh that packet from the checked-out files for each increment.
- The primary orchestrator is the sole editor of shared roadmap status during a parallel task and reconciles reviewer findings before changing it. Specialists return evidence and do not create conflicting progress snapshots.
- Gemini CLI discovers project specialists from .gemini/agents/. The available reviewers are spring-reviewer, angular-reviewer, database-reviewer, and security-reviewer. Their descriptions identify when they apply.
- Specialist agents have read-only file and search tools. They analyze code and return evidence; they do not edit files or start other agents. The primary session delegates only bounded, relevant reviews and checks any finding against the source before acting on it.
- Use /deliver for a complete implementation workflow and /review for a read-only review of current changes. Use @spring-reviewer, @angular-reviewer, @database-reviewer, or @security-reviewer to request a focused audit directly.
- The agents inherit the Gemini session's model. Keep project-level settings minimal; do not pin a provider model or widen the specialists' tool access without a project need.
- In Gemini CLI, trust the repository workspace, then verify agents with /agents list and commands with /commands list. After adding or editing definitions in a running session, use /agents reload and /commands reload.

## Milestone commits and pushes

- After a milestone satisfies its acceptance criteria and required verification, commit the focused milestone on its topic branch and push that branch to `origin`, as the user requested. Keep each milestone in a reviewable Conventional Commit.
- Do not push to the default branch, force-push, or mark an increment complete while required evidence is missing. Record blocked checks in the issue, roadmap, release-readiness page, and changelog as appropriate.
