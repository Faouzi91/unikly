# Gemini CLI project context

@./AGENTS.md

Reusable project skills live in .agents/skills/. Project-specific read-only specialist agents live in .gemini/agents/. The main Gemini session is the orchestrator: it owns implementation and only delegates bounded reviews to the relevant specialist. Do not invoke every reviewer for every task.

Use /deliver for a complete shippable implementation workflow and /review for a read-only audit of current changes. Direct specialist prompts are available as @spring-reviewer, @angular-reviewer, @database-reviewer, and @security-reviewer.

Use docs/UNIKLY_SRS_v4.md as the active requirements/status source and docs/product-roadmap.md as the shared increment sequence. For each delegated review, provide the same issue, requirement IDs, roadmap increment/status, acceptance criteria, and changed paths. The primary session integrates evidence and is the only writer of roadmap status during parallel work. After accepted and verified milestones, follow AGENTS.md's topic-branch commit-and-push rule.

Trust this repository workspace when prompted. Check loaded instructions with /memory show, available specialists with /agents list, commands with /commands list, and skills with /skills list. After editing definitions in a running session, use /agents reload, /commands reload, or /skills reload as applicable.
