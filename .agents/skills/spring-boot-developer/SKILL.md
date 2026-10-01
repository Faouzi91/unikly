---
name: spring-boot-developer
description: >-
  Enterprise-grade best practices for modern Spring Boot (v3.4/v4.x) and Java 25 development in Unikly.
  Use when creating or refactoring backend controllers, services, repositories, domain entities,
  Spring Security configurations, or exception handlers.
---

# Spring Boot Developer Skill (Java 25 & Spring Boot 3.4/4.x)

This skill provides architectural patterns, coding guidelines, and enterprise standards for developing the Unikly backend.

## Architectural Boundaries

Unikly follows a modular monolith architecture. Code is partitioned by business domain (`identity`, `catalog`, `orders`, `profile`) under `com.unikly.store.*`. Each domain maintains strict layer separation:

1. **Domain (`domain/`)**:
   - Entities (`@Entity`, `@Table`) and Enums (`StoreRole`, `DeliveryMethod`, etc.).
   - Encapsulate domain logic, state validation, and business invariants directly within entities.
   - Protect primary keys and immutable timestamps. Use `BigDecimal` with 2 decimal places for all financial values.

2. **Persistence (`persistence/`)**:
   - Spring Data JPA repositories extending `JpaRepository<Entity, ID>`.
   - Prefer derived query methods (e.g. `findAllBySellerIdOrderByCreatedAtDesc`) or explicit JPQL/HQL queries.
   - Never expose raw database entities directly to external consumers; always map through service layer DTOs.

3. **Application / Service (`application/`)**:
   - Transaction boundaries: Annotate service classes with `@Transactional` (Spring Framework).
   - Read-only operations must use `@Transactional(readOnly = true)` to avoid unnecessary Hibernate dirty checking.
   - Request and response data structures must be defined as immutable Java **records** (e.g., `ProductRequests.Upsert`, `OrderRequests.Create`).

4. **API / Controllers (`api/`)**:
   - Clean REST endpoints using `@RestController` and explicit request mapping paths (`/api/...`).
   - Validate incoming requests with `@Valid` and Jakarta validation annotations (`@NotBlank`, `@Min`, `@Size`, etc.).
   - Explicit HTTP status codes (`@ResponseStatus(HttpStatus.CREATED)`, `@ResponseStatus(HttpStatus.NO_CONTENT)`).

---

## Spring Security & Authentication Best Practices

1. **Session & CSRF Lifecycle**:
   - Unikly uses stateful, session-based authentication backed by Spring Security with `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
   - State-changing requests (`POST`, `PUT`, `DELETE`, `PATCH`) require the `X-XSRF-TOKEN` header matching the session's cookie.
   - When users log in, Spring Security rotates the session ID. Clients must re-fetch `GET /api/auth/csrf` to retrieve the new session-bound CSRF token.

2. **Role-Based Access Control**:
   - Standard roles: `StoreRole.BUYER`, `StoreRole.SELLER`, `StoreRole.ADMIN`.
   - Use method security (`@PreAuthorize("hasAuthority('ROLE_SELLER')")`) or route security filters.
   - Always verify resource ownership: A seller may only update or delete products they own (`sellerId == authenticatedUser.id`); a buyer may only cancel or confirm orders they placed.

---

## Exception Handling Best Practices

1. **Scoped Exception Handlers**:
   - When writing `@RestControllerAdvice`, **always scope** to specific controllers or packages (e.g. `@RestControllerAdvice(assignableTypes = AuthController.class)`).
   - **Never** register global catch-all handlers that intercept generic database exceptions (like `DataIntegrityViolationException`) and report false errors (such as "An account with this email already exists") for unrelated operations like order placement or catalog creation.

2. **Standard HTTP Error Responses**:
   - Use `ResponseStatusException` (e.g., `HttpStatus.NOT_FOUND`, `HttpStatus.FORBIDDEN`, `HttpStatus.BAD_REQUEST`) for standard domain rejections.
   - Include helpful, non-sensitive error messages.

---

## Financial & Calculation Accuracy

- **Never use `double` or `float` for prices, delivery fees, or order totals.**
- Always use `java.math.BigDecimal` with `RoundingMode.HALF_UP` and explicit 2 decimal places.
- Keep delivery fee and subtotal calculations strictly authoritative on the backend. Never trust client-supplied totals.
