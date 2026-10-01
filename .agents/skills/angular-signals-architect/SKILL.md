---
name: angular-signals-architect
description: >-
  Official Angular best practices for modern standalone architecture, Signals, and reactive UI development.
  Use when creating or refactoring Angular components, templates, services, routing, forms, or UI layouts.
---

# Angular Signals Architect Skill (Angular 22.x & TypeScript)

This skill enforces official Angular standards, modern reactivity patterns, and frontend best practices tailored for the Unikly web client.

## Modern Component Architecture

1. **Standalone Components Only**:
   - Every component, pipe, and directive must be `standalone: true` (or omitted in modern Angular where standalone is the default).
   - Never generate or introduce `NgModules`. Explicitly import required components, directives, and pipes in the component's `imports: [...]` array.

2. **Constructor-less Dependency Injection**:
   - Use the `inject()` function to inject services rather than constructor parameters:
     ```typescript
     private readonly ordersService = inject(OrdersService);
     private readonly authState = inject(AuthStateService);
     private readonly router = inject(Router);
     ```

3. **Modern Control Flow (`@if`, `@for`, `@switch`)**:
   - Never use deprecated structural directives like `*ngIf`, `*ngFor`, or `*ngSwitch`.
   - Always supply a tracking expression in `@for`:
     ```html
     @for (product of sortedProducts(); track product.id) {
       <article class="product-item">...</article>
     } @empty {
       <p class="empty-state">No products found.</p>
     }
     ```

---

## State Management with Angular Signals

1. **Reactive State**:
   - Use `signal<T>()` for mutable local state:
     ```typescript
     readonly deliveryMethod = signal<DeliveryMethod>('STANDARD');
     readonly isSubmitting = signal<boolean>(false);
     ```
2. **Derived State with `computed()`**:
   - Compute prices, totals, or filtered lists with `computed()` to avoid manual subscription sync:
     ```typescript
     readonly deliveryFee = computed(() => this.deliveryMethod() === 'EXPRESS' ? 15.00 : (this.subtotal() >= 50 ? 0.00 : 5.00));
     readonly total = computed(() => this.subtotal() + this.deliveryFee());
     ```
3. **Side Effects with `effect()`**:
   - Use `effect()` only when synchronizing state with external APIs or local storage, never for setting other signals that could be derived via `computed()`.

---

## Defensive Templates & Asset Handling

1. **Image Fallbacks**:
   - External images (e.g. Unsplash, user-uploaded URLs) can fail or 404 over the network.
   - Always attach a fallback error handler and set `loading="lazy"`:
     ```html
     <img [src]="product.image" [alt]="product.name" loading="lazy" (error)="$any($event.target).src = '/product-placeholder.svg'">
     ```

2. **Accessibility (WCAG 2.1 AA)**:
   - Ensure all interactive elements have accessible names (`[attr.aria-label]`).
   - Use semantic elements (`<main>`, `<article>`, `<section>`, `<nav>`, `<header>`).
   - Mark dynamic updates or status alerts with `role="status"` or `aria-live="polite"`.

3. **Styling & Design Tokens**:
   - Use scoped CSS referencing the project's CSS variables (e.g., `--color-surface`, `--color-primary`, `--radius-md`).
   - Never use `::ng-deep` or hardcoded inline styles unless dynamically calculated.
