---
name: web-security-owasp
description: >-
  Review and implement security controls in Unikly authentication, authorization, API mutations, ownership, CSRF, sensitive data, and configuration.
---

# Unikly web security

Use this skill for security-sensitive changes and release reviews. Treat [docs/authorization-matrix.md](../../../docs/authorization-matrix.md) and the running code as the authority for current permissions and routes; this checklist is not a substitute for checking the actual matcher and service.

## Authentication and sessions

- Passwords are hashed with Argon2. Never store, log, or return plaintext passwords or password hashes.
- Authentication is session-based. Keep session cookies HttpOnly and use the configured SameSite policy. Do not put bearer tokens or session IDs in browser storage.
- State-changing requests require CSRF protection. Keep the CSRF cookie/header behavior aligned with AuthSecurityConfiguration; refresh the token after login rotates the session ID.
- Keep account registration limited to supported public roles. Do not add a public admin role assignment or role-change route.

## Authorization and ownership

- New /api/** routes must be explicitly authorized; unmatched API paths are denied by default.
- Grant the narrowest permission needed and update the authorization matrix when route mappings change.
- Derive the acting customer from the authenticated principal. Verify ownership of each order, cart, profile, review, product, or order item in the application service.
- Seller role access to a route does not establish ownership of a product or order item.
- Verify anonymous, allowed-role, denied-role, and cross-account access for authorization changes.

## Input, output, and business rules

- Validate untrusted request data at the API boundary and enforce domain invariants in the application/domain layer.
- Use parameterized database operations. Never build SQL/JPQL from untrusted string concatenation.
- Do not render untrusted content as HTML or bypass Angular sanitization.
- Compute prices, inventory-sensitive checkout, delivery fees, and order totals on the server. Use BigDecimal in Java.
- Validate external URLs before server-side fetching; use allowlists where practical to prevent SSRF.

## Secrets and error handling

- Keep credentials and signing keys in ignored local configuration or a managed secret store. Keep .env.example safe to commit.
- Avoid displaying local .env values in tool output, reports, or chat.
- Return generic safe errors to clients. Keep SQL messages, traces, session values, and private user data out of responses and logs.
- Keep exception mapping scoped to the module or controller.

## Review checklist

- Is every new API path covered by an explicit permission rule?
- Does the service enforce ownership even when the caller has the endpoint permission?
- Are login, logout, password changes, and other mutations protected by the correct CSRF/session behavior?
- Can a request body or browser state influence the acting user, role, price, or total?
- Are secrets, session values, personal data, and internal error details protected?
- Are access-control and business-invariant tests updated for the change?
