# Changelog

Progress notes for the Unikly project. Payment collection and live shipment tracking remain out of scope for the current demo checkout.

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
