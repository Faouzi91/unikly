---
name: git-workflow
description: >-
  Preserve Unikly user work and keep changes isolated with safe topic branches, deliberate staging, reviewable commits, and careful handoff. Use for Git operations or delivery preparation.
---

# Git workflow

Keep changes isolated and reviewable. Preserve all pre-existing tracked changes and untracked files.

## Before editing

- Run git status --short --branch. Inspect the current branch and both tracked and untracked changes before editing.
- Do not overwrite, stage, move, or remove user-owned work that is unrelated to the task.
- Keep non-trivial work on a focused topic branch using feat/, fix/, refactor/, docs/, or chore/ prefixes. Do not switch branches if it would risk existing work.
- Branch from the current local base unless the user asks to synchronize with a remote. Do not fetch or pull automatically.

## While working

- Keep the change set scoped. Stage explicit paths only if a commit was requested; never stage everything blindly.
- Do not commit unless asked. Do not amend, reset, clean, force-push, merge, or push unless the user requests that action.
- Never use destructive Git operations to make a working tree look clean.
- If asked to commit, use a concise Conventional Commit message and inspect the staged diff first.
- Do not include secrets, local environment files, generated build output, or unrelated editor configuration in a commit.

## Before handoff

- Inspect git diff --check, git diff --stat, the tracked diff, and status for untracked files.
- Confirm only intended files changed and that user work remains untouched.
- Report the branch, whether changes were committed, checks actually performed, and any remaining verification gap.
