---
name: feature-delivery
description: >-
  Plan, implement, review, and verify a Unikly feature, bug fix, or refactor as a small shippable slice. Use for multi-file or behavior-changing work.
---

# Feature delivery and orchestration

Use this workflow to keep work grounded in Unikly's active product and shippable in small increments. The primary session owns the outcome, implementation, reviewer coordination, and final report.

## 1. Ground the task

- Check Git status and preserve existing tracked edits and untracked files.
- Read the relevant implementation, tests, product requirements, and current architecture before editing.
- Read docs/UNIKLY_SRS_v4.md for current requirements and status and docs/product-roadmap.md for sequence and progress. Follow the linked GitHub issue as the active increment contract; also read README.md, docs/architecture.md, and docs/authorization-matrix.md as relevant.
- Treat SRS v2/v3 and the old onboarding guide as historical. Do not scope this product from them. Payment, carrier, settlement, and service-decomposition work remains gated by explicit requirements and roadmap decisions.

## 2. Define a small complete slice

For behavior changes, identify the user outcome, acceptance conditions, affected layers, and risks. Prefer one complete user path over broad framework work. Resolve routine implementation choices from current code and requirements; ask only when a material product or data decision cannot be inferred.

## 3. Coordinate the right specialists

- The primary session is the orchestrator. In Gemini CLI, use the local read-only agents in .gemini/agents when their expertise matches the changed code.
- Keep Codex, Gemini, and every delegated reviewer on the same versioned task packet: active issue, SRS requirement IDs, roadmap increment/status, acceptance criteria, and changed paths. The primary session reads the latest repository progress files, gives that exact context to reviewers, reconciles their results, and updates the shared roadmap once.
- If scope, acceptance, changed paths, or roadmap status changes while reviews are running, send the same updated packet to each affected reviewer and request a fresh review before integrating stale findings.
- Delegate only bounded, independent research or review. Give each specialist the user outcome, exact changed paths, relevant requirements, and a specific question.
- Run independent reviews in parallel when that saves time. Do not delegate short tasks, dependent steps, or edits to the same files.
- Available reviewers: spring-reviewer for Java/Spring; angular-reviewer for Angular; database-reviewer for JPA, PostgreSQL, and Flyway; security-reviewer for authentication, authorization, CSRF, ownership, secrets, and sensitive data.
- Specialists have read-only tools and cannot delegate further. They report evidence and findings; the primary session makes changes, verifies findings against the implementation, resolves disagreements, and remains accountable for the result.
- Invoke only reviewers relevant to the change. A copy-only documentation edit does not need every specialist.

## 4. Implement within existing boundaries

- For a testable bug, reproduce it with a focused failing test before changing implementation.
- For a feature, derive focused tests from the acceptance conditions and cover the user-visible behavior and relevant invariants.
- Change only the layers required by the acceptance conditions.
- Keep Flyway migrations forward-only and server validation, authorization, ownership, and business calculations authoritative.
- Update user-facing docs and CHANGELOG.md when behavior or maintained project guidance changes.
- Keep open work in a GitHub issue using the work-item template; record acceptance and verification evidence in the pull request template. Update docs/release-readiness.md when a release gate changes state. CHANGELOG.md records completed work only.
- Follow docs/product-roadmap.md milestone rules. After all acceptance and required verification are satisfied, make a focused Conventional Commit and push the topic branch to origin per the user's standing request. Never push directly to the default branch or claim completion with a required check outstanding.

## 5. Review and verify

- Review the final diff for unrelated edits, failure states, access control, data ownership, migration safety, accessibility, responsive behavior, secrets, accidental dependencies, and claims unsupported by code.
- For security-sensitive, high-risk, or broad changes, request an independent read-only specialist review after implementation.
- Choose verification that matches the changed layer:
  - Backend changes: focused Maven tests in the Java 25 Docker toolchain.
  - Frontend changes: relevant Angular tests and production build in the Node 24 Docker toolchain.
  - Schema, authentication, authorization, checkout, orders, or cross-layer changes: smoke-test the affected flow against the local Compose stack.
  - Documentation-only changes: inspect source, links, and the final diff; an application build or live-stack run is unnecessary.
- Follow the Docker workflow in ../docker-toolchain-isolated/SKILL.md and the local-flow checklist in ../e2e-smoke-testing/SKILL.md.
- Record the exact checks and results. State clearly when a relevant check was not run or was blocked.
- Do not mark a release gate complete until the named build, test, or live-flow evidence exists.
