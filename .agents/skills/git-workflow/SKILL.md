---
name: git-workflow
description: >-
  Git workflow, branching conventions, semantic commit standards, and merge procedures for Unikly.
  Use when starting new features, bug fixes, refactoring, or preparing pull requests and commits.
---

# Git Workflow & Branching Guidelines

This skill enforces structured git branch management, semantic commit standards, and safe integration procedures for the Unikly platform.

---

## 1. Branching Strategy

Never commit directly to `master` for non-trivial increments, bug fixes, or feature development. Always create focused, short-lived topic branches from the latest `master`.

### Branch Naming Conventions

| Branch Prefix | Purpose | Example |
| :--- | :--- | :--- |
| `feat/` | New functionality, domain models, or user features | `feat/product-reviews`, `feat/cart-quantity-stepper` |
| `fix/` | Bug fixes, regression remedies, or display corrections | `fix/star-rating-color`, `fix/csrf-session-expiry` |
| `refactor/` | Code structure improvements with no behavior changes | `refactor/signals-cart-store`, `refactor/security-config` |
| `perf/` | Performance optimizations, SQL index tuning | `perf/catalog-aggregate-query` |
| `docs/` | Architectural documentation, roadmap, or rule updates | `docs/update-build-guide` |
| `chore/` | Dependency upgrades, build configuration adjustments | `chore/upgrade-lucide-icons` |

---

## 2. Standard Workflow Lifecycle

### Step 1: Synchronize and Branch
```bash
git checkout master
git pull --rebase origin master
git checkout -b <branch-type>/<short-description>
```

### Step 2: Develop and Test in Isolation
- Follow toolchain isolation constraints (never run `mvn` or `npm` directly on the host):
  ```bash
  # Backend verification in Docker
  docker run --rm -v "${HOME}/.m2:/root/.m2" -v "$(pwd)/backend:/workspace" -w /workspace unikly-api-test mvn test

  # Frontend verification in Docker
  docker build -t unikly-web-test ./frontend
  ```

### Step 3: Semantic Commit Messages
Commit atomically using the [Conventional Commits](https://www.conventionalcommits.org/) format:
```bash
<type>(<scope>): <concise present-tense description>
```
Common scopes: `catalog`, `identity`, `orders`, `reviews`, `cart`, `frontend`, `security`, `docs`.

Example:
```bash
git add .
git commit -m "fix(frontend): restore star icon fill colors via encapsulated deep styles"
```

### Step 4: Live Verification
Run end-to-end smoke tests against the running Docker Compose stack:
```bash
python3 /home/aboubakar-garba/.gemini/antigravity-cli/brain/d0e22077-c86a-4e78-90ef-5f0df8179ce6/scratch/smoke_test.py
```

### Step 5: Merge and Rebase
Once all verification steps pass:
```bash
git checkout master
git merge --ff-only <branch-type>/<short-description>
git push origin master
git branch -d <branch-type>/<short-description>
```

---

## 3. Hygiene & Safety Rules

1. **No Sensitive Data**: Never commit secrets, credentials, API keys, or machine-specific absolute file paths.
2. **Clean Status**: Ensure `git status` shows zero untracked artifacts or unwanted temporary files before committing.
3. **Commit Integrity**: Never amend or force-push commits already pushed to shared remotes unless explicitly approved.
