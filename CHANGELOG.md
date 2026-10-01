# Changelog

Progress notes for the Unikly project. Payment collection and carrier integrations remain out of scope for the current demo checkout.

## 2026-10-01 — Delivery options and profile address auto-fill

- Auto-fill buyer delivery contact and address details from saved customer profile upon entering checkout.
- Added delivery method options (Standard Delivery with free shipping over $50 vs. Express Delivery for $15.00).
- Authoritatively calculate delivery fees and order totals on the server.
- Persist delivery method and fee with the order; expose delivery method in buyer and seller order management views.
- Seed catalog products for the dev seller account (`seller@unikly.local`) and reassign sample items to prevent order item seller foreign key constraint violations.
- Scoped `AuthExceptionHandler` to `AuthController` to avoid obscuring unrelated database constraint errors.
- Fixed 404 Unsplash image URL for the Anodized Aluminum Mechanical Pencil Set product via migration V13 and `DevelopmentUserSeeder`.
- Added image load error fallback handlers across product grid, detail view, and seller cards to gracefully fall back to `/product-placeholder.svg`.

## 2026-10-01 — Buyer order cancellation

- Let buyers cancel only their own orders while every item is still waiting to be processed.
- Restore the canceled items' inventory and expose cancellation in buyer order history.
- Mark the order and its items as canceled, and protect the endpoint with `ORDER_CANCEL_SELF` plus buyer ownership checks.

## 2026-09-30 — Allow buyer delivery confirmation

- Authorize the order delivery confirmation route for buyers with order read access.
- Keep the endpoint protected by buyer ownership checks in the order service.

## 2026-09-30 — Buyer delivery confirmation

- Let buyers confirm delivery after every item in their order has shipped.
- Update all shipped items to delivered so the buyer and each seller see the completed order state.
- Restrict confirmation to the buyer who placed the order and reject premature confirmations.

## 2026-09-30 — Seller shipment tracking details

- Ask sellers for a carrier and tracking URL when they mark an order as shipped.
- Validate tracking URLs as absolute HTTP or HTTPS links and save details with the seller's order items.
- Show buyers a Track package link for shipped items in order history.

## 2026-09-30 — Honor API port in Angular dev proxy

- Read the API port from the ignored root `.env` file so Angular dev requests follow the Compose port mapping.
- Avoid sending authenticated API requests to a stale default port when `API_HOST_PORT` is customized.

## 2026-09-30 — Buyer item fulfillment visibility

- Include each item's seller fulfillment status in buyer order history.
- Clarify the seller's progress separately for items in mixed-seller orders.

## 2026-09-29 — Seller order fulfillment

- Add a seller-only order inbox that includes delivery details and only the seller's own order lines.
- Let sellers advance their items through placed, processing, shipped, and delivered states.
- Show buyers the order's least-complete fulfillment state across all sellers.
- Enforce seller ownership and one-step status transitions on the server.

## 2026-09-29 — Remove committed development passwords

- Require local environment configuration for the database password and optional seeded development accounts.
- Disable development account seeding by default and document `.env` setup without publishing passwords.
- Generate temporary passwords at test runtime instead of storing reusable test credentials in the repository.

## 2026-09-29 — Expired checkout session recovery

- Detect an expired buyer session during checkout, keep the basket, and prompt the buyer to sign in again.
- Clarify the cause: development API restarts invalidate in-memory login sessions.

## 2026-09-29 — Persist featured catalog products

- Seed the storefront sample products into the shared database catalog so checkout can reserve their inventory.
- Merge API records with the featured presentation data by product ID to prevent duplicate cards and keep server stock authoritative.
- Return a not-found error for stale basket product IDs instead of reporting a stock conflict.

## 2026-09-29 — Persistent orders and purchase history

- Save orders and delivery details to the signed-in buyer account, retaining a stable order reference and purchased-item snapshots.
- Decrease product inventory atomically when an order is accepted; reject an order if stock has changed.
- Add a buyer order history view with placed date, items, quantity, total, and order status.
- Explain that shipment tracking and payment are not integrated yet.

## 2026-09-29 — Demo checkout

- Added a delivery details form reachable from the basket.
- Added an order summary, client-side stock availability validation, and a demo confirmation reference.
- Clear the basket after a successful demo order.
- Clearly identify checkout as a demo without payment or delivery processing. (Order persistence and saved delivery details were added in the following increment.)

## 2026-09-29 — Product stock and availability

- Added seller-managed stock quantities to product creation and editing.
- Added stock availability indicators to product listings and details.
- Prevented basket quantities from exceeding the available stock.
- Added the database migration that introduced stock quantities for existing catalog listings.
