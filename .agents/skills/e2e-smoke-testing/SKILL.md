---
name: e2e-smoke-testing
description: >-
  Procedures and test patterns for executing end-to-end smoke tests against the running Unikly stack.
  Use before concluding any feature, minor increment, bugfix, or release to verify live system behavior.
---

# End-to-End Smoke Testing Skill

This skill outlines how to execute and verify end-to-end smoke tests against the running Unikly environment.

## Guiding Principle: "Prove It Before Concluding"

Never assume code is working based on compilation or isolated unit tests alone. Before declaring any task or increment complete, you must execute a live smoke test verifying the entire request lifecycle (Frontend -> Nginx -> Spring Boot -> PostgreSQL).

---

## Key Authentication & Security Requirements

1. **CSRF Token Rotation**:
   - Calling `POST /api/auth/login` causes Spring Security to rotate the HTTP session ID.
   - Any client or test script must call `GET /api/auth/csrf` to retrieve the updated `XSRF-TOKEN` cookie for the authenticated session before attempting any state-changing requests (`POST`, `PUT`, `DELETE`).
   - The value of `XSRF-TOKEN` must be forwarded in the `X-XSRF-TOKEN` HTTP header.

2. **Standard Development Accounts**:
   Configured via `.env` with `UNIKLY_SEED_DEV_USERS=true`:
   - **Buyer**: `customer@unikly.local` / `UniklyCustomer2026!`
   - **Seller**: `seller@unikly.local` / `UniklySeller2026!`
   - **Admin**: `admin@unikly.local` / `UniklyAdmin2026!`

---

## Core Smoke Test Checklist

Every full smoke test run should validate:

1. **CSRF Initialization**: Retrieve baseline CSRF cookie.
2. **Buyer Authentication**: Log in as `customer@unikly.local`, verify `BUYER` role and profile details.
3. **Profile Operations**: Update and retrieve default delivery address and contact information.
4. **Catalog Inspection**: Query `GET /api/products`, ensuring products have valid prices, categories, and working image URLs.
5. **Order Placement (Standard)**: Place an order using `STANDARD` delivery method. Verify server-authoritative fee calculation ($5.00 flat fee or $0.00 if subtotal ≥ $50.00) and check that `seller_id` is properly assigned on order items.
6. **Order Placement (Express)**: Place an order using `EXPRESS` delivery method. Verify server-side flat fee of $15.00.
7. **Order History**: Check `GET /api/orders/mine` and confirm placed orders display correct delivery methods and fees.
8. **Buyer Cancellation**: Cancel an unfulfilled order via `PUT /api/orders/mine/{reference}/cancel`. Verify status updates to `CANCELED`.
9. **Seller Inbox**: Log in as `seller@unikly.local`, query `GET /api/orders/seller`, and confirm seller sees assigned items with customer delivery details.
10. **Seller Products**: Query `GET /api/products/mine` to confirm seller owns their active catalog inventory.
11. **Frontend Availability**: Perform HTTP GET on `http://localhost:4200` to verify Nginx and the Angular application render `<app-root>`.

---

## Python Smoke Test Execution

A complete verification script can be executed against the running Compose services:

```bash
python3 scratch/smoke_test.py
```
Expected output:
```text
ALL 11 END-TO-END SMOKE TESTS PASSED WITH 100% SUCCESS!
```
