---
name: web-security-owasp
description: >-
  Security hardening, vulnerability prevention, and OWASP Top 10 compliance for Unikly.
  Use when designing authentication flows, implementing data mutations, reviewing endpoints for IDOR,
  configuring CORS/CSRF, or performing security reviews before release.
---

# Web Security & OWASP Hardening Skill

This skill provides security review protocols, threat modeling directives, and hardening rules based on the OWASP Top 10 and enterprise Spring Boot/Angular security patterns.

---

## 1. Access Control & Authorization (OWASP A01: Broken Access Control)

1. **Object-Level Authorization (IDOR Prevention)**:
   - Never rely solely on URL parameters (e.g. `/api/orders/{ref}`) without enforcing ownership in the service layer.
   - **Buyer Boundaries**:
     - When fetching or modifying an order (`/api/orders/mine/...`), verify `order.buyerId == currentUser.id`.
     - When updating profile details (`/api/profile/me`), derive user identity strictly from `Authentication.getName()`.
   - **Seller Boundaries**:
     - When a seller updates or deletes a product (`PUT /api/products/{id}`), verify `product.sellerId == currentUser.id`.
     - When fulfilling or updating order tracking (`PUT /api/orders/seller/...`), ensure the seller only updates line items where `item.sellerId == currentUser.id`.

2. **Role-Based Access Control (RBAC)**:
   - Ensure every protected controller route enforces appropriate authorities:
     - `BUYER`: Can place orders, view personal order history, cancel unfulfilled orders, update personal delivery profiles.
     - `SELLER`: Can manage their own product inventory, view assigned orders, update shipment tracking.
     - `ADMIN`: Platform moderation, account suspension, audit logs.

---

## 2. Authentication & Session Hygiene (OWASP A07: Identification Failures)

1. **Password Storage**:
   - Always hash passwords using `BCryptPasswordEncoder` with default strength (10+ rounds).
   - Never store, log, or return plain text passwords or password hashes in API responses.

2. **CSRF Protection & Token Rotation**:
   - Unikly enforces stateful CSRF protection via `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
   - Spring Security rotates the session ID upon login (`POST /api/auth/login`).
   - Clients and test runners must fetch `GET /api/auth/csrf` to retrieve the active session's `XSRF-TOKEN` cookie and send it in the `X-XSRF-TOKEN` request header for all state-changing verbs (`POST`, `PUT`, `DELETE`).

---

## 3. Data & Injection Defense (OWASP A03: Injection & XSS)

1. **SQL & HQL Injection**:
   - Always use Spring Data JPA derived query methods or parameterized HQL (`@Query("... WHERE p.sellerId = :sellerId")`).
   - **Never concatenate raw strings into JPQL, HQL, or native SQL queries.**

2. **Cross-Site Scripting (XSS)**:
   - Angular automatically contextually encodes template bindings (e.g. `{{ product.name }}`).
   - **Prohibited**: Never use `[innerHTML]` with unsanitized user content or call `DomSanitizer.bypassSecurityTrustHtml` on user-supplied text.
   - Clean and trim text inputs upon receipt in the backend service layer before persistence.

---

## 4. Authoritative Business Logic (OWASP A04: Insecure Design)

1. **Financial Immutability**:
   - Clients must **never** dictate product prices, delivery fees, or order totals.
   - Calculate all totals authoritatively on the backend using `BigDecimal` with 2 decimal places and `RoundingMode.HALF_UP`.
2. **State Machine Invariants**:
   - Enforce valid state transitions at the service level:
     - Orders can only be canceled by the buyer while in `PLACED` state before any item is fulfilled.
     - Orders can only be marked `SHIPPED` by the assigned seller.

---

## 5. Secret Hygiene & Configuration (OWASP A05: Security Misconfiguration)

1. **Environment & Secrets**:
   - Passwords, database URLs, and cryptographic secrets must live in `.env` (which is strictly gitignored).
   - `.env.example` must contain only empty or safe development placeholders.
2. **Scoped Exception Handling**:
   - Restrict `@RestControllerAdvice` to targeted controllers.
   - Never expose internal database stack traces, SQL errors, or sensitive environment details in HTTP 500 responses.
3. **External Resource Validation**:
   - When storing external URLs (e.g. image URLs, carrier tracking URLs), validate that schemes are strictly `http://` or `https://` with valid hosts to prevent SSRF and protocol manipulation.

---

## Security Verification Checklist (Pre-Merge Audit)

- [ ] Does every state-changing route require valid authentication and CSRF token?
- [ ] Does the service explicitly verify that the caller owns the resource (IDOR check)?
- [ ] Are all financial calculations performed authoritatively on the backend using `BigDecimal`?
- [ ] Are all database queries parameterized without string concatenation?
- [ ] Are external images and links validated for safe `http(s)` protocols?
- [ ] Are secrets excluded from git commits and API response payloads?
