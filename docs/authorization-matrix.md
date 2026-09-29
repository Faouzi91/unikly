# Authorization Model

Authorization is enforced by Spring Security on the server. Angular route guards and displayed permissions are for navigation and clarity only; they are never the security boundary.

## Role matrix

| Capability | Buyer | Seller | Administrator |
| --- | --- | --- | --- |
| Read and update own account | Yes | Yes | Yes |
| Browse catalog | Yes | Yes | No by default |
| Manage own cart | Yes | No by default | No by default |
| Create, read, and cancel own orders | Yes | No by default | No by default |
| Read any account | No | No | Yes |
| Suspend accounts | No | No | Yes |
| Manage catalog | No | No | Yes |
| Manage orders | No | No | Yes |
| Issue refunds | No | No | Yes |
| Moderate reviews | No | No | Yes |
| Assign roles | No | No | Yes |
| Read platform reports | No | No | Yes |

“Own” resources must be scoped to the authenticated principal in the service/repository query; possession of an object ID is not authorization. Admin actions should be separately permission-checked and audited. A broad admin role is not a substitute for checking the permission needed by a specific operation.

## Permission identifiers

| Permission | Intended use |
| --- | --- |
| `ACCOUNT_READ_SELF` | Read the authenticated customer's own account |
| `ACCOUNT_UPDATE_SELF` | Change allowed fields on own account |
| `CATALOG_READ` | Browse published products |
| `CART_MANAGE_SELF` | Add/remove/change own cart items |
| `ORDER_CREATE_SELF` | Submit own checkout/order |
| `ORDER_READ_SELF` | Read own order history and status |
| `ORDER_CANCEL_SELF` | Cancel own eligible order |
| `PLATFORM_ADMIN` | Platform administration capability; not a default substitute for narrower checks |
| `ACCOUNT_READ_ANY` | Read any customer account in admin workflows |
| `ACCOUNT_SUSPEND` | Suspend or restore accounts |
| `CATALOG_MANAGE` | Create/update/archive products and categories |
| `ORDER_MANAGE` | Manage order fulfillment and status |
| `ORDER_REFUND` | Issue or approve refunds |
| `REVIEW_MODERATE` | Moderate user reviews |
| `ROLE_ASSIGN` | Assign roles through a trusted administrative workflow |
| `PLATFORM_REPORT_READ` | Read platform-wide reports; currently gates the sample admin access-check route |

## Current API policy

- Public: `GET /api/auth/csrf`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /actuator/health`.
- Authenticated with `ACCOUNT_READ_SELF`: `GET /api/auth/me`.
- Authenticated with `ACCOUNT_READ_SELF`: `GET /api/profile/me`.
- Authenticated with `ACCOUNT_UPDATE_SELF`: `PUT /api/profile/me` and `POST /api/auth/password`.
- Authenticated with `PLATFORM_REPORT_READ`: `GET /api/admin/access-check`.
- Every other `/api/**` endpoint is denied until it is explicitly assigned a permission.
- Registration accepts only `BUYER` or `SELLER` as `accountType`; arbitrary roles, including `ADMIN`, cannot be assigned through public registration. No public role-change endpoint exists.
- Role assignment and initial administrator provisioning are trusted operator tasks. Never seed a known admin password into a migration or image.

## Rules for adding endpoints

1. Add a narrowly named permission to `StorePermission` only when a real operation needs it.
2. Grant it to the minimum role(s) in `StoreRole`.
3. Require that permission in the HTTP matcher or a method-level authorization annotation.
4. For customer-owned data, verify ownership in the application/service query as well as the endpoint permission.
5. Add tests for allowed roles, denied roles, anonymous requests, and cross-account resource access.
6. Do not use role/permission information from request bodies, query parameters, or browser state as authority.
7. Keep UI guards in sync for usability, but test server enforcement independently.
