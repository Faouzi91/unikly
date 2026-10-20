# AI agent workflow and ECC skill mapping

This repository keeps shared, project-specific skills in .agents/skills/ and uses Gemini CLI's native project agents under .gemini/agents/. The main session owns delivery; specialists provide focused review evidence. All contributors use the same active SRS and roadmap rather than maintaining separate agent-specific plans.

## Shared project state

- Active requirement/status source: [docs/UNIKLY_SRS_v4.md](UNIKLY_SRS_v4.md).
- Increment sequence and progress: [docs/product-roadmap.md](product-roadmap.md).
- Active work and acceptance: the linked GitHub issue and pull request.
- Architecture and permission contract: [docs/architecture.md](architecture.md) and [docs/authorization-matrix.md](authorization-matrix.md).
- Launch evidence: [docs/release-readiness.md](release-readiness.md); completed work only: root `CHANGELOG.md`.
- The primary session sends every relevant reviewer the same versioned task packet: issue, requirement IDs, roadmap increment/status, acceptance checklist, and changed paths. Reviewers are read-only; the primary resolves findings and updates shared progress once. If scope, acceptance, or roadmap status changes while reviews are running, send the same updated packet to every affected reviewer and get a fresh response before merging findings.

## ECC skills that fit Unikly

ECC has useful source skills for this stack. Their broad guidance is adapted into Unikly's current rules and toolchains rather than copied as a second, competing rulebook.

| Work area | Relevant ECC skills | Unikly source of truth |
| --- | --- | --- |
| Spring Boot and Java | springboot-patterns, springboot-security, springboot-tdd, springboot-verification, java-coding-standards, jpa-patterns | .agents/skills/spring-boot-developer/SKILL.md, .agents/skills/web-security-owasp/SKILL.md, .agents/skills/feature-delivery/SKILL.md |
| Angular storefront | angular-developer | .agents/skills/angular-signals-architect/SKILL.md |
| PostgreSQL and schema changes | postgres-patterns, database-migrations, jpa-patterns | .agents/skills/postgres-flyway-architect/SKILL.md |
| Git | git-workflow | .agents/skills/git-workflow/SKILL.md |
| Customer-flow verification | tdd-workflow, e2e-testing, verification-loop | .agents/skills/feature-delivery/SKILL.md, .agents/skills/docker-toolchain-isolated/SKILL.md, .agents/skills/e2e-smoke-testing/SKILL.md |
| Orchestration | team-agent-orchestration | .agents/skills/feature-delivery/SKILL.md and the primary Gemini session |

Useful upstream references:

- [ECC Spring Boot patterns](https://github.com/affaan-m/ECC/blob/main/skills/springboot-patterns/SKILL.md), [Spring security](https://github.com/affaan-m/ECC/blob/main/skills/springboot-security/SKILL.md), [Java standards](https://github.com/affaan-m/ECC/blob/main/skills/java-coding-standards/SKILL.md), and [JPA patterns](https://github.com/affaan-m/ECC/blob/main/skills/jpa-patterns/SKILL.md).
- [ECC Angular developer](https://github.com/affaan-m/ECC/blob/main/skills/angular-developer/SKILL.md).
- [ECC PostgreSQL patterns](https://github.com/affaan-m/ECC/blob/main/skills/postgres-patterns/SKILL.md) and [database migrations](https://github.com/affaan-m/ECC/blob/main/skills/database-migrations/SKILL.md).
- [ECC Git workflow](https://github.com/affaan-m/ECC/blob/main/skills/git-workflow/SKILL.md), [TDD workflow](https://github.com/affaan-m/ECC/blob/main/skills/tdd-workflow/SKILL.md), and [team-agent orchestration](https://github.com/affaan-m/ECC/blob/main/skills/team-agent-orchestration/SKILL.md).

ECC's frontend-patterns skill targets React and Next.js. Use its Angular-specific angular-developer skill as reference instead, while keeping the installed Angular version and current application patterns authoritative. Do not impose ECC's blanket coverage targets or shell commands where they conflict with Unikly's focused validation and Docker-only toolchain rules.

## Gemini delivery roles

- Use /deliver for a scoped implementation. The primary session reads the feature-delivery skill, inspects the working tree, implements the change, asks only relevant specialists for independent review, integrates findings, and reports checks.
- Use /review for a read-only audit of current changes.
- spring-reviewer audits Java and Spring changes; angular-reviewer audits Angular changes; database-reviewer audits JPA, PostgreSQL, and Flyway changes; security-reviewer audits authentication, authorization, CSRF, ownership, secrets, and sensitive data.
- Specialist agents have read-only file and search tools. They do not implement, run shell commands, or call other agents. The main session remains the single integrator.
- Skip specialists for small, isolated work that does not benefit from independent review. For cross-layer work, delegate only the independent areas touched; do not fan out to every agent by default.
- The agents inherit the selected Gemini session model. No project model override or global settings change is needed. Reviewers do not need shell access to inspect supplied paths and related source.

## Delivery gates

- Follow the current product scope and module boundaries in AGENTS.md and docs/architecture.md.
- Use forward-only Flyway migrations and verify real customer-flow, authorization, and persistence behavior against the local Compose stack when the change affects those areas.
- Run backend and frontend checks in the pinned Docker workflows. H2-based tests do not prove PostgreSQL-specific behavior.
- Review the full tracked diff and inspect untracked files without staging or deleting user work.
- Keep each task on a focused topic branch. After a milestone passes its acceptance and required verification, commit it with a Conventional Commit and push the topic branch to `origin`, per the user's standing instruction. Do not push to the default branch or force-push.
