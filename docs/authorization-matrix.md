# Authorization model

The backend is the security boundary. Frontend route guards and displayed permissions are for usability only.

## Current role behavior

| Capability | Buyer | Seller | Administrator |
| --- | --- | --- | --- |
| Read and update own account/profile | Yes | Yes | Yes |
| Browse products and public reviews | Yes | Yes | Yes |
| Manage own cart and place orders | Yes | No | No |
| Read, cancel, and confirm own orders | Yes | No | No |
| Read and fulfill orders containing own products | No | Yes | No |
| Manage own catalog products | No | Yes, with ownership checks | No current route |
| Create a public review | Yes, after a `DELIVERED` order line for that product | No | No |
| Moderate any product review | No | No | Yes |
| Use platform administration APIs | No | No | Permission-gated; only the access-check route is currently implemented |

Public catalog and review reads do not require a role. Role names are not a replacement for checking ownership of a specific resource.

## Current permission and route mapping

| Route or operation | Current rule |
| --- | --- |
| GET /api/auth/csrf, POST /api/auth/register, GET /actuator/health | Public |
| POST /api/auth/login | Public, CSRF required; login attempts are rate-limited |
| POST /api/auth/verification/request, /api/auth/verification/confirm, /api/auth/password-recovery/request, /api/auth/password-recovery/confirm | Public, CSRF required; requests are rate-limited (429 Too Many Requests when exceeded) and responses do not reveal whether an email is registered |
| POST /api/auth/logout | Session logout; state-changing requests require CSRF |
| GET /api/auth/me, GET /api/profile/me | ACCOUNT_READ_SELF |
| PUT /api/profile/me, POST /api/auth/password | ACCOUNT_UPDATE_SELF |
| /api/cart/** | CART_MANAGE_SELF; the service uses the authenticated customer |
| POST /api/orders | ORDER_CREATE_SELF; prices and totals are computed server-side |
| GET /api/orders/mine and buyer delivery confirmation | ORDER_READ_SELF; the service scopes the order to its buyer |
| Buyer order cancellation | ORDER_CANCEL_SELF; only eligible orders owned by the buyer may be canceled |
| Seller order list and status updates | ORDER_FULFILL_OWN; each affected order item must belong to the seller. Seller order views omit buyer email and expose recipient phone only when the delivery method requires it (`EXPRESS`) |
| GET /api/products/mine | Seller role |
| POST, PUT, and DELETE /api/products/** | Seller role; seller ownership is checked for product mutations |
| GET /api/products and public product/review reads | Public |
| GET /api/reviews/summary | Public aggregate rating summaries for catalog cards |
| POST /api/products/{productId}/reviews, DELETE .../reviews/mine | REVIEW_CREATE_SELF; creation also requires a non-canceled order with a `DELIVERED` order line for `productId` owned by the authenticated buyer |
| DELETE /api/reviews/{id} | REVIEW_MODERATE |
| GET /api/admin/access-check | PLATFORM_REPORT_READ |
| Other /api/admin/** routes | PLATFORM_ADMIN |
| Any unmatched /api/** route | Denied |

The route rules above reflect AuthSecurityConfiguration. Admins moderate customer reviews through REVIEW_MODERATE; they cannot create or delete a customer review through the buyer endpoints. When changing a route or permission, inspect that configuration, the controller, and the application service, then update this document and add access-control tests.

## Permission inventory

The permission names are defined in StorePermission. The current routes use ACCOUNT_READ_SELF, ACCOUNT_UPDATE_SELF, CART_MANAGE_SELF, ORDER_CREATE_SELF, ORDER_READ_SELF, ORDER_CANCEL_SELF, ORDER_FULFILL_OWN, REVIEW_CREATE_SELF (Buyer only), REVIEW_MODERATE (Administrator only), PLATFORM_REPORT_READ, and PLATFORM_ADMIN.

Other enum values are reserved until an endpoint uses them. Do not describe an enum permission as an implemented capability without confirming the route mapping and service behavior.

## Rules for new endpoints

1. Add the narrowest permission needed for a real operation and grant it only to the required role.
2. Add an explicit HTTP matcher or method-level authorization. Preserve the deny-by-default rule.
3. Scope customer-owned reads and mutations to the authenticated principal in the application service or repository query.
4. For seller-owned products and order items, verify the seller owns each affected resource.
5. Test anonymous, allowed-role, denied-role, and cross-account access when the change affects authorization.
6. Never trust role, permission, customer ID, seller ID, price, or total values supplied by the browser.
7. Keep Angular guards aligned for usability, but verify access control on the server.
