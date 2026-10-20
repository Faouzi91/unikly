# Unikly storefront theme and style audit

**Reviewed:** 2026-10-20
**Scope:** Angular styles/templates and source-level interaction patterns. This was a code inspection; the running UI was not visually inspected in a browser.

## Overall assessment

The current storefront has a recognizable home-oriented direction: restrained green branding, white/pale-gray/muted-green surfaces, and editorial lifestyle imagery. That gives Unikly a credible foundation. The theme is not yet consistently expressed across the entire product: the shell is green, identity calls to action use a yellow marketplace pattern, and reviews introduce another slate/amber treatment. Repeated literal colors and page-level styles make the visual language difficult to maintain. The best next step is a focused consistency and accessibility pass, not a wholesale brand redesign.

## Findings ordered by user and trust impact

| Priority | Finding | Evidence / impact | Recommended correction |
| --- | --- | --- | --- |
| P1 | Trust promises exceed implemented policy | Storefront and basket promise free delivery above $50 and 30-day returns. The home page also says “Secure checkout / your details stay protected,” while checkout explicitly says no payment is collected and production TLS/secrets readiness remains open. | Tie every delivery, return, and security statement to a published policy and implemented flow. State clearly that checkout places an unpaid order until payment exists; defer payment-security claims until an approved live-payment flow. |
| P1 | Sample offers are presented like ordinary seller offers | `core/catalog/sample-products.ts` supplies hard-coded prices, stock, ratings, and review counts; the catalog client retains sample fallback/merge behavior, while storefront cards show normal prices and badges without a demo marker. Buyers can mistake fixture data for a real seller offer. | Label sample offers clearly or remove them from public catalog behavior; show seller provenance for real offers. Record M4 completion evidence before a public release. |
| P1 | Review eligibility shown by the UI does not match the purchase rule | Product detail derives `canReview` from BUYER/ADMIN role and displays “You ordered this item” / verified-review wording, although the SRS requires an agreed completed eligible purchase. The server must remain authoritative, but the UI currently overstates eligibility. | Derive the CTA and status from server-reported eligibility after the completion-state policy is approved; keep the backend eligibility check and verified marker aligned. See M3. |
| P1 | Review picker is not a complete accessible radio control | Product detail markup sets `role="radiogroup"` on a container and `aria-checked` on buttons without radio roles or arrow-key/roving-focus behavior. | Use native labeled radio inputs or implement the complete radio-group keyboard and ARIA pattern. Test keyboard and screen-reader state. |
| P1 | Category selection is visual-only | Home category buttons style `.active` but do not expose selected state with `aria-pressed` or another suitable semantic. | Add an accessible selected state and verify focus, keyboard activation, and announcement. |
| P2 | Brand colors and control patterns drift between features | Shell uses green/neutral colors; auth uses yellow CTAs; reviews use slate/amber. Feature CSS repeats color literals and there is no common token foundation. | Add semantic CSS custom properties for brand, text/surface, border, status, focus, spacing, radius, type scale, and control sizes. Migrate shell/home/catalog first, then other features in small slices. |
| P2 | Motion preferences are not handled consistently | Smooth scrolling and transitions exist; no `prefers-reduced-motion` override was found. | Add a global reduced-motion rule and ensure component-specific animation does not override it. |
| P2 | Several mobile controls are smaller than comfortable touch targets | Header/nav links, category chips, cart steppers, and auth buttons are commonly around 32–38px tall. | Aim for 44px primary touch targets where layout permits; preserve spacing and check compact layouts at 320px and 375px. |
| P2 | Hero and product media rely on remote Unsplash URLs | The hero lacks explicit load priority and fallback; product images also use remote URLs. Product cards/details already reserve image space with aspect ratios and have a local error fallback; the hero has a fixed/minimum-height reservation but no fallback. Network failure can still remove prominent content and external assets can change. | Prefer owned or stable optimized assets, preserve current aspect-ratio/height reservations, refine intrinsic/responsive sizing, set intentional LCP priority for the hero, and verify fallback behavior. |
| P3 | Browser title is fixed to sign-in wording | `frontend/src/index.html` sets a title that does not represent storefront/product routes. | Set route-aware page titles and useful descriptions as catalog routes are improved. |
| P3 | Storefront contains hard-coded fixture categories and local-only filtering | Category labels and filter/sort behavior live in the page; this is hard to reconcile with seller-managed catalog data. | As part of M9/M10, source search and categories from the API while preserving URL state and useful no-result states. |

## Incremental design work

### T1 — Trust and interaction semantics

- Reconcile free-delivery, returns, secure-checkout, and payment wording with the actual selected policy and implementation.
- Identify or remove sample offers and fixture ratings so they cannot be mistaken for verified seller inventory.
- Make review eligibility and visible status depend on the server's approved completed-purchase decision; preserve server enforcement.
- Fix category selected state and implement a keyboard-operable review rating control.
- Give each user action a visible focus, pressed/selected, disabled, error, and success state as appropriate.

### T2 — Shared visual foundation

- Add central semantic tokens: brand/action, text/surface, border, feedback colors, focus ring, spacing, radii, typography scale, and control-height conventions.
- Add reduced-motion behavior and check text, icons, borders, and focus contrast in default/hover/focus/disabled states.
- Migrate global shell, home, and catalog first; keep existing brand direction while standardizing implementation.

### T3 — Feature consistency and responsiveness

- Migrate basket/checkout, identity/account, and seller pages to the shared tokens and reusable button/form/status patterns.
- Increase touch targets and verify long names, error text, navigation, forms, and price summaries at narrow mobile widths.
- Use route-aware titles, stable/local media, retain current image-space reservations, refine responsive sizing, and add intentional hero loading priority/fallback behavior.

### T4 — Browser acceptance

- Inspect real browser views at 320px, 375px, tablet, and desktop widths.
- Keyboard-test navigation, dialogs/menus if present, product rating entry, filters, basket quantity changes, checkout, and seller actions.
- Check contrast and focus appearance; test reduced-motion preference and image failure.
- Record screenshots/viewport and browser evidence in the milestone issue/PR. Source inspection alone does not close this gate.

## Files and limitations

Audit evidence included `frontend/src/styles.css`, `frontend/src/app/app.css`, storefront/home, catalog/product detail, cart/basket, checkout, identity/auth, and `frontend/src/index.html`. The rendered site was not started or viewed, so spacing/contrast observations from source should be confirmed in a real browser before calling the visual milestone done. No frontend behavior was changed in this audit.
