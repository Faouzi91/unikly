# AGENT.md — Senior Fullstack Engineering Orchestrator

> **Stack:** Spring Boot 3.4+ · Angular 19+ · PostgreSQL 16+ · Docker  
> **Philosophy:** Library-first. Never hand-code what a battle-tested library solves.  
> **Audience:** AI agents (Claude, Codex, Gemini, Cursor, Antigravity, etc.)

---

## Table of Contents

1. [Core Philosophy](#core-philosophy)
2. [The 3-Layer Engineering Model](#the-3-layer-engineering-model)
3. [AI Generation Discipline](#ai-generation-discipline)
4. [AI Reliability & Hallucination Prevention](#ai-reliability--hallucination-prevention)
5. [Technology Stack & Version Pinning](#technology-stack--version-pinning)
6. [Backend: Spring Boot Standards](#backend-spring-boot-standards)
7. [Database: PostgreSQL & Migration Strategy](#database-postgresql--migration-strategy)
8. [API Design Principles](#api-design-principles)
9. [Security](#security)
10. [Frontend: Angular Standards](#frontend-angular-standards)
11. [Docker & Containerization](#docker--containerization)
12. [Testing Strategy](#testing-strategy)
13. [Observability & Monitoring](#observability--monitoring)
14. [CI/CD Pipeline](#cicd-pipeline)
15. [Git Workflow Discipline](#git-workflow-discipline)
16. [Project Structure](#project-structure)
17. [Documentation](#documentation)
18. [Coding Challenge Mode](#coding-challenge-mode)
19. [AI Code Review Checklist](#ai-code-review-checklist)
20. [Final Rule](#final-rule)

---

## Core Philosophy

Before generating code:

1. Understand the requirements
2. Design the architecture
3. Generate code in controlled steps
4. Verify correctness
5. Iterate safely

**Cardinal rules:**

- Never generate large systems blindly.
- Prefer the simplest solution that satisfies requirements. Readable code > clever code.
- Use established libraries for every solved problem. Never hand-code CSV parsing, JSON formatting, object mapping, date formatting, or any utility that a maintained library already provides.
- Every design decision must be justifiable. If asked "why?", the AI must have a concrete answer.

---

## The 3-Layer Engineering Model

### Layer 1 — Directive (Requirements)

Business requirements provided by the user: entities, endpoints, views, workflows, business rules.

The AI must not invent features beyond what is required.

### Layer 2 — Orchestration (Architectural Thinking)

Before writing code the AI must:

1. Analyze the directive
2. Design system architecture (draw it out if helpful)
3. Define data flow end-to-end
4. Decide file organization
5. Identify all dependencies and infrastructure needs
6. State the chosen runtime strategy explicitly

The AI acts as a software architect at this stage.

### Layer 3 — Execution (Code Generation)

Once architecture is defined: generate deterministic, strongly typed, readable code with no unnecessary complexity.

---

## AI Generation Discipline

The AI must **never** generate an entire system at once.

**Controlled generation order:**

1. Database schema + Flyway migrations
2. Domain models / JPA entities
3. DTOs and API contracts
4. MapStruct mappers
5. Repository layer
6. Service / business logic
7. API controllers
8. Global exception handling
9. Security configuration
10. OpenAPI documentation annotations
11. Backend tests (unit + integration)
12. Angular models, services, interceptors
13. Angular UI components + routing
14. Frontend tests
15. Docker & Docker Compose
16. README and deployment docs

**After each step:**

- Verify compilation (`./gradlew build` or `ng build`)
- Integrate into the running system
- Commit changes with a semantic message

---

## AI Reliability & Hallucination Prevention

Before accepting generated code, verify:

- The library/dependency exists, is maintained, and is compatible with the current stack versions
- The solution follows framework conventions (Spring Boot auto-configuration, Angular DI, etc.)
- No framework feature is manually reimplemented
- Edge cases are handled (nulls, empty collections, invalid input, concurrent access)
- Code compiles and tests pass
- No deprecated APIs are used

**If unsure whether a library or API exists, say so.** Never fabricate an import or method signature.

---

## Technology Stack & Versioning Policy

**Always use the latest stable release of every dependency.** The AI must never default to an older version when a newer stable release exists. If unsure of the current latest stable, the AI must verify before generating `build.gradle.kts` or `package.json`.

**Version resolution rule:** When generating dependency files, use the latest stable version available at the time of generation. Do not hardcode versions from training data — check official sources (Maven Central, npmjs.com, Docker Hub) if in doubt. Use Spring Boot's dependency management BOM to align transitive dependency versions automatically.

### Backend

| Concern               | Choice                         | Version Policy            |
|------------------------|--------------------------------|---------------------------|
| Framework              | Spring Boot                    | Latest stable release     |
| Language               | Java (latest LTS)              | Latest LTS (e.g., 21, 25+) |
| Build Tool             | Gradle (Kotlin DSL)            | Latest stable release     |
| Database               | PostgreSQL                     | Latest stable release     |
| Migration              | Flyway                         | Latest stable release     |
| ORM                    | Spring Data JPA / Hibernate    | Managed by Boot BOM       |
| Mapping                | MapStruct                      | Latest stable release     |
| Validation             | Jakarta Validation (Hibernate Validator) | Managed by Boot BOM |
| JSON                   | Jackson                        | Managed by Boot BOM       |
| CSV                    | OpenCSV or Apache Commons CSV  | Latest stable release     |
| Excel                  | Apache POI                     | Latest stable release     |
| API Docs               | SpringDoc OpenAPI (Swagger UI) | Latest stable release     |
| Caching                | Spring Cache + Caffeine (local) or Redis (distributed) | Latest stable release |
| Logging                | SLF4J + Logback                | Managed by Boot BOM       |
| Testing                | JUnit 5 + Mockito + Testcontainers | Managed by Boot BOM (Testcontainers: latest stable) |

### Frontend

| Concern               | Choice                         | Version Policy            |
|------------------------|--------------------------------|---------------------------|
| Framework              | Angular                        | Latest stable release     |
| Language               | TypeScript                     | Latest stable (as supported by Angular) |
| State Management       | NgRx (Signal Store preferred for new projects) or Angular Signals | Latest stable release |
| HTTP                   | Angular HttpClient + interceptors | Bundled with Angular   |
| Forms                  | Reactive Forms (never template-driven for complex forms) | Bundled with Angular |
| UI Library             | Angular Material or PrimeNG    | Latest stable release     |
| CSS Framework          | Tailwind CSS                   | Latest stable release     |
| Alerts / Dialogs       | SweetAlert2 (`sweetalert2` + `@sweetalert2/ngx-sweetalert2`) | Latest stable release |
| Toast Notifications    | ngx-toastr                     | Latest stable release     |
| Loading Spinners       | ngx-spinner                    | Latest stable release     |
| CSS                    | SCSS                           | —                         |
| Testing                | Jest (unit) + Cypress or Playwright (e2e) | Latest stable release |
| Linting                | ESLint + Prettier              | Latest stable release     |

### Infrastructure

| Concern               | Choice                         | Version Policy            |
|------------------------|--------------------------------|---------------------------|
| Containerization       | Docker + Docker Compose        | Latest stable release     |
| Reverse Proxy (prod)   | Nginx (serves Angular, proxies API) | Latest stable alpine image |
| Base JDK Image         | Eclipse Temurin                | Latest LTS JDK            |
| Base Node Image        | Node (Alpine)                  | Latest LTS release        |
| CI/CD                  | GitHub Actions or GitLab CI    | Latest action versions    |

### Why "Latest Stable" and Not Pinned Versions

- Pinned versions in an AI orchestration file go stale within weeks.
- The AI's job is to generate code with the best tools available *now*, not 6 months ago.
- Spring Boot's BOM already handles transitive version alignment — trust it.
- The `gradle.lockfile` and `package-lock.json` in the actual project are where exact pins belong, not in this directive.

### Packaging Rule

Spring Boot REST APIs must be packaged as executable **JARs**. Never use WAR packaging or generate `ServletInitializer` unless deploying to a legacy application server.

---

## Backend: Spring Boot Standards

### Architecture

```
Controller → Service → Repository → Database
```

**Rules:**

- Controllers remain thin — validate input, delegate to service, return response.
- Services contain all business logic and transaction boundaries.
- Repositories manage persistence only. No business logic in repositories.
- DTOs define API contracts. Entities never leak to the API layer.

### Dependency Injection

Constructor injection only. Never use `@Autowired` on fields.

```java
@Service
@RequiredArgsConstructor  // Lombok generates the constructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
}
```

### DTO-First Design

Define request/response contracts before implementation.

```java
public record CreateTaskRequest(
    @NotBlank String title,
    @Size(max = 500) String description,
    @NotNull @FutureOrPresent LocalDate dueDate
) {}

public record TaskResponse(
    Long id,
    String title,
    String description,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dueDate,
    TaskStatus status
) {}
```

**DTO Rules:**

- Use Java `record` for immutable DTOs (Java 16+).
- Validation annotations live on request DTOs.
- Jackson annotations (`@JsonFormat`, `@JsonProperty`) for serialization control.
- **Never** convert `LocalDate`/`LocalDateTime` to `String` in DTOs. Keep native types; let Jackson handle formatting.

### Object Mapping

**Use MapStruct exclusively.** Never write manual `fromEntity()` / `toEntity()` methods.

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskResponse toResponse(Task task);
    Task toEntity(CreateTaskRequest request);
    void updateEntity(UpdateTaskRequest request, @MappingTarget Task task);
}
```

### Date & Time Handling

- Use `java.time` exclusively: `LocalDate`, `LocalDateTime`, `Instant`, `ZonedDateTime`.
- Never use `java.util.Date` or `java.sql.Date`.
- Store timestamps as `TIMESTAMPTZ` in PostgreSQL, mapped to `Instant` or `ZonedDateTime` in Java.
- Configure Jackson globally:

```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: true
```

### Transaction Management

- Annotate service methods with `@Transactional` for write operations.
- Use `@Transactional(readOnly = true)` for read-only operations (enables Hibernate optimizations).
- Never place `@Transactional` on controllers or repositories.
- For methods that mix reads and writes, the outer method is `@Transactional` and inner read methods benefit from it.

```java
@Transactional
public TaskResponse createTask(CreateTaskRequest request) { ... }

@Transactional(readOnly = true)
public Page<TaskResponse> getTasks(Pageable pageable) { ... }
```

### Global Exception Handling

Use `@RestControllerAdvice` for centralized error handling. Never let stack traces leak to clients.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_FAILED", errors));
    }
}
```

### Logging Discipline

- Use SLF4J via Lombok's `@Slf4j`.
- Log meaningful operations in services: creation, updates, deletions, external calls.
- Log at appropriate levels: `info` for business events, `warn` for recoverable issues, `error` for failures with stack traces.
- Never log sensitive data (passwords, tokens, PII).
- Use structured logging with key-value pairs for production (JSON format via Logback encoder).

```java
log.info("Task created: id={}, assignee={}", task.getId(), task.getAssignee());
log.error("Payment processing failed: orderId={}", orderId, exception);
```

### Caching Strategy

- Use Spring Cache abstraction (`@Cacheable`, `@CacheEvict`, `@CachePut`).
- Default to Caffeine for local/single-instance caching.
- Use Redis (via Spring Data Redis) when distributed caching is needed.
- Always define explicit cache names and TTLs. Never use unbounded caches.

```java
@Cacheable(value = "tasks", key = "#id")
public TaskResponse getTask(Long id) { ... }

@CacheEvict(value = "tasks", key = "#id")
public TaskResponse updateTask(Long id, UpdateTaskRequest request) { ... }
```

### Performance Rules

- Avoid N+1 queries: use `@EntityGraph` or JPQL `JOIN FETCH`.
- Add database indexes on frequently queried columns.
- Use pagination for all list endpoints (Spring `Pageable`).
- Avoid unnecessary database calls — batch operations where possible.
- Use `@Query` with projections for read-heavy endpoints that don't need full entities.

---

## Database: PostgreSQL & Migration Strategy

### Schema Management with Flyway

**Every schema change goes through Flyway.** Never use `spring.jpa.hibernate.ddl-auto=update` in any environment beyond initial prototyping.

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate  # Validate schema matches entities; never auto-modify
```

**Migration file naming:**

```
V1__create_task_table.sql
V2__add_status_column_to_task.sql
V3__create_user_table.sql
```

**Migration rules:**

- Migrations are immutable once committed. Never edit a released migration.
- Each migration does one logical thing.
- Always include rollback considerations in comments.
- Use `TIMESTAMPTZ` for timestamps, never `TIMESTAMP WITHOUT TIME ZONE`.
- Define constraints at the database level (NOT NULL, UNIQUE, FK, CHECK).

### PostgreSQL-Specific Best Practices

- Use `TEXT` instead of `VARCHAR(n)` unless a hard length limit is a business rule (PostgreSQL treats them identically for performance).
- Use `BIGSERIAL` or `BIGINT` with sequences for primary keys. Consider UUIDs (`gen_random_uuid()`) for distributed systems.
- Add indexes explicitly in migrations for foreign keys and frequently filtered columns.
- Use `JSONB` for semi-structured data (not `JSON` — `JSONB` is indexable and faster for queries).
- Connection pooling: use HikariCP (Spring Boot default) with sensible pool sizes.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

---

## API Design Principles

### REST Conventions

```
GET    /api/v1/tasks           → list (paginated)
GET    /api/v1/tasks/{id}      → get by ID
POST   /api/v1/tasks           → create
PUT    /api/v1/tasks/{id}      → full update
PATCH  /api/v1/tasks/{id}      → partial update
DELETE /api/v1/tasks/{id}      → delete
```

### API Versioning

Prefix all endpoints with `/api/v1/`. This allows non-breaking evolution and future versioning.

### Response Wrapping

**Never return a raw JSON array.** Always wrap in a response object.

```json
{
  "data": [...],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

This prevents JSON hijacking and allows adding metadata without breaking clients.

### HTTP Status Codes

| Code | Usage                                      |
|------|--------------------------------------------|
| 200  | Successful GET, PUT, PATCH                 |
| 201  | Successful POST (resource created)         |
| 204  | Successful DELETE (no content)             |
| 400  | Validation failure or malformed request    |
| 401  | Unauthenticated                            |
| 403  | Authenticated but unauthorized             |
| 404  | Resource not found                         |
| 409  | Conflict (duplicate, version mismatch)     |
| 500  | Unhandled server error                     |

### Pagination

All list endpoints must support pagination via Spring `Pageable`.

```
GET /api/v1/tasks?page=0&size=20&sort=createdAt,desc
```

### API Documentation

Use SpringDoc OpenAPI for automatic Swagger UI generation.

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x.x'
```

Annotate controllers with `@Operation`, `@ApiResponse`, `@Tag` for rich documentation.

Access Swagger UI at `/swagger-ui.html` during development.

### CORS Configuration

Configure CORS explicitly in a `@Configuration` class. Never use `@CrossOrigin` on individual controllers.

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

## Security

### General Principles

- Validate all inputs (Jakarta Validation on DTOs).
- Sanitize all outputs.
- Never leak internal details (stack traces, SQL errors, server versions).
- Use Spring Security for authentication/authorization when required.
- Apply the principle of least privilege.

### Secrets Management

Never hardcode secrets. Use environment variables or externalized configuration.

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Secrets that must never be committed:**

- Database credentials
- JWT secrets / signing keys
- API keys for external services
- OAuth client secrets

Use `.env` files for local development (added to `.gitignore`).

### Spring Security Configuration (when required)

Use the modern `SecurityFilterChain` bean approach. Never extend `WebSecurityConfigurerAdapter` (removed in Spring Security 6).

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())  // Disable for stateless APIs with JWT
        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated()
        )
        .build();
}
```

---

## Frontend: Angular Standards

### Architecture

```
Angular App
├── core/           → Singleton services, guards, interceptors (provided in root)
├── shared/         → Reusable components, pipes, directives
├── features/       → Feature modules (lazy-loaded)
│   ├── tasks/
│   │   ├── components/
│   │   ├── services/
│   │   ├── models/
│   │   └── tasks.routes.ts
│   └── auth/
└── environments/   → Environment-specific configuration
```

### Key Conventions

- **Standalone components** (Angular 19+ default). No NgModules for new components.
- **Reactive Forms** for any form with validation or dynamic behavior.
- **Typed HTTP Client**: Always define interfaces for API responses. Never use `any`.
- **Lazy loading**: Every feature is lazy-loaded via the router.
- **OnPush change detection** for all components by default.
- **Signals** for local component state (Angular 17+). Consider NgRx Signal Store for shared state.

### HTTP Layer

Centralize API communication with a typed service per resource.

```typescript
@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/tasks`;

  getTasks(page = 0, size = 20): Observable<PagedResponse<Task>> {
    return this.http.get<PagedResponse<Task>>(this.apiUrl, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  createTask(request: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, request);
  }
}
```

### HTTP Interceptors

Use functional interceptors (Angular 17+):

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        inject(Router).navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
```

Register in `app.config.ts`:

```typescript
provideHttpClient(withInterceptors([authInterceptor, errorInterceptor]))
```

### Environment Configuration

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: '/api'  // Relative — Nginx proxies to backend
};
```

### Angular Linting & Formatting

- ESLint with `@angular-eslint` for consistent code quality.
- Prettier for formatting. Configure once, enforce everywhere.
- Strict TypeScript: `strict: true` in `tsconfig.json`. No exceptions.

### Tailwind CSS Integration

Tailwind CSS is the utility-first CSS framework for this stack. Install and configure it with Angular:

```bash
npm install -D tailwindcss @tailwindcss/forms @tailwindcss/typography
npx tailwindcss init
```

**Rules:**

- Use Tailwind utility classes for all layout and styling. Avoid writing custom CSS unless absolutely necessary.
- Use `@tailwindcss/forms` plugin for consistent form element styling.
- Use `@tailwindcss/typography` plugin for rich text / prose content.
- Configure `content` paths in `tailwind.config.js` to cover all Angular template and component files.
- Use Tailwind's built-in dark mode support (`darkMode: 'class'`) when dark mode is required.
- Never mix Tailwind with another CSS framework (no Bootstrap alongside Tailwind).

### Alerts & Confirmation Dialogs — SweetAlert2

Use **SweetAlert2** (`sweetalert2` + `@sweetalert2/ngx-sweetalert2`) for all confirmation dialogs, alerts, and user-facing modal notifications. It is WAI-ARIA accessible, zero-dependency, actively maintained, and has an official Angular integration.

```bash
npm install sweetalert2 @sweetalert2/ngx-sweetalert2
```

**Setup (standalone / app.config.ts):**

```typescript
import { provideSweetAlert2 } from '@sweetalert2/ngx-sweetalert2';

export const appConfig: ApplicationConfig = {
  providers: [
    provideSweetAlert2(),
    // ...
  ]
};
```

**Usage rules:**

- Use SweetAlert2 for confirmations (delete, submit, destructive actions), error alerts, and success notifications that require user acknowledgment.
- Use the `<swal>` component or `[swal]` directive for template-driven dialogs — never call `Swal.fire()` directly from components (use the Angular wrapper for change detection safety).
- Use `*swalPortal` for Angular template content inside modals (data binding, directives, etc.).
- Apply a SweetAlert2 theme from `@sweetalert2/themes` to match the application's design system, or customize SCSS variables.
- Never use browser-native `alert()`, `confirm()`, or `prompt()`.

### Toast Notifications — ngx-toastr

Use **ngx-toastr** for all non-blocking, ephemeral toast notifications (success, error, warning, info). It is the most widely adopted Angular toast library, supports AOT, standalone apps, and Angular animations.

```bash
npm install ngx-toastr
```

**Setup (standalone / app.config.ts):**

```typescript
import { provideToastr } from 'ngx-toastr';
import { provideAnimations } from '@angular/platform-browser/animations';

export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimations(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
    }),
    // ...
  ]
};
```

**Usage rules:**

- Use `ToastrService` via DI — never instantiate manually.
- Call `this.toastr.success()`, `.error()`, `.warning()`, `.info()` in services or components.
- Use toasts for transient feedback (record saved, action completed, validation error). For actions requiring user acknowledgment, use SweetAlert2 instead.
- Customize toast styles via Tailwind-compatible CSS overrides, not inline styles.
- Configure global defaults in `provideToastr()`. Override per-toast only when necessary.

### Loading Spinners — ngx-spinner

Use **ngx-spinner** for all loading states (page loads, API calls, lazy-loaded routes). It offers 50+ spinner animations, supports multiple named instances, fullscreen and container-scoped modes.

```bash
npm install ngx-spinner
```

**Setup (standalone / app.config.ts):**

```typescript
import { NgxSpinnerModule } from 'ngx-spinner';

// In your app.config.ts or root component imports:
// imports: [NgxSpinnerModule.forRoot({ type: 'ball-clip-rotate' })]
```

**Usage rules:**

- Use `NgxSpinnerService` via DI: `this.spinner.show()` / `this.spinner.hide()`.
- Use named spinners for concurrent loading zones (e.g., `this.spinner.show('tableSpinner')`).
- Integrate with HTTP interceptors to show/hide spinners automatically during API calls.
- For container-scoped spinners (`fullScreen: false`), ensure the parent element has `position: relative`.
- Import only the CSS animation files you actually use (in `angular.json` styles array) to minimize bundle size.
- Never build custom spinner components when ngx-spinner provides the animation you need.

### When to Use Which Notification Pattern

| Scenario                                  | Use This        |
|-------------------------------------------|-----------------|
| Record saved, action completed            | ngx-toastr      |
| API error, validation failure (transient) | ngx-toastr      |
| "Are you sure?" before delete/submit      | SweetAlert2     |
| Success with details requiring reading    | SweetAlert2     |
| Error requiring user acknowledgment       | SweetAlert2     |
| Page or section loading                   | ngx-spinner     |
| API call in progress                      | ngx-spinner     |
| Lazy route loading                        | ngx-spinner     |

---

## Docker & Containerization

### Backend Dockerfile (Multi-Stage Build)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon  # Cache dependencies
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile (Multi-Stage Build)

```dockerfile
# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

# Stage 2: Serve
FROM nginx:alpine
COPY --from=build /app/dist/*/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:80 || exit 1
```

### Nginx Configuration (Frontend)

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # Angular routing — serve index.html for all non-file requests
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to backend
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Docker Compose

```yaml
services:
  database:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-appdb}
      POSTGRES_USER: ${DB_USERNAME:-appuser}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-apppass}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-appuser} -d ${DB_NAME:-appdb}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://database:5432/${DB_NAME:-appdb}
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME:-appuser}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-apppass}
      SPRING_PROFILES_ACTIVE: docker
    depends_on:
      database:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      backend:
        condition: service_healthy

volumes:
  postgres_data:
```

### Docker Rules

- **Always use multi-stage builds** to keep images small.
- **Always use health checks** on every service.
- **Always use `depends_on` with `condition: service_healthy`** to ensure correct startup order.
- **Never run containers as root** — create a non-root user in the Dockerfile.
- **Never store data in containers** — use volumes for persistence.
- **Use `.dockerignore`** to exclude `node_modules`, `.git`, `build/`, `dist/`.

---

## Testing Strategy

### Backend Testing Pyramid

| Layer              | Tool                      | What to Test                              |
|--------------------|---------------------------|-------------------------------------------|
| Unit               | JUnit 5 + Mockito         | Service logic, mappers, utilities          |
| Slice              | `@WebMvcTest`             | Controller request/response, validation    |
| Integration        | `@SpringBootTest` + Testcontainers | Full flow with real PostgreSQL   |
| Repository         | `@DataJpaTest` + Testcontainers | JPA queries, custom repository methods |

### Testcontainers for PostgreSQL

Never mock the database for integration tests. Use Testcontainers to spin up a real PostgreSQL instance.

```java
@SpringBootTest
@Testcontainers
class TaskServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldCreateAndRetrieveTask() { ... }
}
```

### Frontend Testing

| Layer    | Tool                  | What to Test                        |
|----------|-----------------------|-------------------------------------|
| Unit     | Jest                  | Services, pipes, pure logic         |
| Component | Jest + Angular Testing Library | Component rendering, interaction |
| E2E      | Cypress or Playwright | Critical user journeys              |

### Testing Rules

- Services must have >80% test coverage for business logic.
- Every API endpoint must have at least one happy-path and one error-path test.
- Tests must be deterministic — no reliance on external services or timing.
- Use factories/builders for test data. Never create test objects with dozens of constructor args.

---

## Observability & Monitoring

### Spring Boot Actuator

Always include Actuator for production readiness.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
  health:
    db:
      enabled: true
```

### Health Checks

Custom health indicators for critical dependencies:

```java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check external dependency
        return Health.up().withDetail("externalApi", "reachable").build();
    }
}
```

### Structured Logging (Production)

Use JSON logging for production environments (machine-parseable by log aggregators).

```xml
<!-- logback-spring.xml -->
<springProfile name="docker,prod">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    <root level="INFO"><appender-ref ref="JSON"/></root>
</springProfile>

<springProfile name="default,dev">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern></encoder>
    </appender>
    <root level="DEBUG"><appender-ref ref="CONSOLE"/></root>
</springProfile>
```

Add `net.logstash.logback:logstash-logback-encoder` as a dependency.

---

## CI/CD Pipeline

### GitHub Actions Example

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports: ["5432:5432"]
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - name: Build & Test
        run: ./gradlew build
        working-directory: backend
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22 }
      - run: npm ci
        working-directory: frontend
      - run: npm run lint
        working-directory: frontend
      - run: npm run test -- --watch=false --browsers=ChromeHeadless
        working-directory: frontend
      - run: npm run build -- --configuration=production
        working-directory: frontend

  docker:
    needs: [backend, frontend]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build & Push Images
        run: docker compose build
```

### Quality Gates

The pipeline must enforce:

- All tests pass
- No compilation errors
- Linting passes (backend checkstyle / frontend ESLint)
- Build artifacts are produced

---

## Git Workflow Discipline

### Branching

- `main` — production-ready code
- `develop` — integration branch
- `feature/*` — individual features
- `bugfix/*` — bug fixes
- `hotfix/*` — urgent production fixes

### Commit Convention

Use semantic commits:

```
feat: add task creation endpoint
fix: resolve N+1 query in task listing
chore: update Spring Boot to 3.4.1
test: add integration tests for task service
docs: update API documentation
refactor: extract task validation logic
```

### Commit Discipline

- Commit after each meaningful milestone from the generation order.
- Each commit must compile and pass tests.
- Never commit broken code, secrets, or generated files that belong in `.gitignore`.

### .gitignore Essentials

```
# Backend
build/
.gradle/
*.jar

# Frontend
node_modules/
dist/

# IDE
.idea/
.vscode/
*.iml

# Environment
.env
*.env.local

# OS
.DS_Store
Thumbs.db
```

---

## Project Structure

### Root

```
project-root/
├── backend/
├── frontend/
├── docker-compose.yml
├── .env.example          ← Document required variables (never .env itself)
├── .gitignore
├── AGENT.md
└── README.md
```

### Backend

```
backend/
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/com/project/
│   │   │   ├── config/           ← CORS, security, caching, OpenAPI
│   │   │   ├── controller/       ← REST controllers (thin)
│   │   │   ├── dto/              ← Request/response records
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── exception/        ← Custom exceptions + global handler
│   │   │   ├── mapper/           ← MapStruct interfaces
│   │   │   ├── model/            ← JPA entities + enums
│   │   │   ├── repository/       ← Spring Data JPA repositories
│   │   │   ├── security/         ← Security config, filters, JWT utils
│   │   │   └── service/          ← Business logic
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-docker.yml
│   │       └── db/migration/     ← Flyway SQL files
│   └── test/
│       └── java/com/project/
│           ├── controller/       ← @WebMvcTest slice tests
│           ├── service/          ← Unit tests (Mockito)
│           ├── repository/       ← @DataJpaTest
│           └── integration/      ← @SpringBootTest + Testcontainers
```

### Frontend

```
frontend/
├── package.json
├── angular.json
├── tsconfig.json
├── Dockerfile
├── nginx.conf
├── src/
│   ├── app/
│   │   ├── core/                 ← Singleton services, guards, interceptors
│   │   │   ├── interceptors/
│   │   │   ├── guards/
│   │   │   └── services/
│   │   ├── shared/               ← Reusable components, pipes, directives
│   │   │   ├── components/
│   │   │   ├── pipes/
│   │   │   └── directives/
│   │   ├── features/             ← Lazy-loaded feature areas
│   │   │   ├── tasks/
│   │   │   │   ├── components/
│   │   │   │   ├── services/
│   │   │   │   ├── models/
│   │   │   │   └── tasks.routes.ts
│   │   │   └── auth/
│   │   ├── app.component.ts
│   │   ├── app.config.ts
│   │   └── app.routes.ts
│   ├── environments/
│   └── styles.scss
```

---

## Runtime Environment & Dependency Sequencing

### Environment-First Rule

The AI must ensure required infrastructure is available before backend execution.

**Startup sequence for database-backed applications:**

1. Define runtime strategy (H2 for rapid dev vs PostgreSQL for production fidelity)
2. Start database (Docker Compose or local)
3. Run Flyway migrations (automatic on boot)
4. Start backend application
5. Verify API endpoints
6. Start frontend development server
7. Verify end-to-end flow

### Development Strategy Selection

| Scenario                | Database Strategy                     |
|------------------------|---------------------------------------|
| Coding challenge       | H2 in-memory for instant startup     |
| Feature development    | PostgreSQL via Docker Compose          |
| Integration testing    | PostgreSQL via Testcontainers          |
| Production             | Managed PostgreSQL (RDS, Cloud SQL)   |

The AI must explicitly state the chosen strategy before generating code.

### Self-Annealing Development Loop

```
Build → Test → Fix → Repeat
```

Backend: `./gradlew build`  
Frontend: `npm install && ng build`

If errors appear: analyze → fix → rebuild → continue.

The AI must not leave broken code for the developer.

---

## Documentation

### README.md Must Include

1. Project overview and purpose
2. Architecture diagram (Mermaid or ASCII)
3. Technology stack with versions
4. Prerequisites (JDK, Node, Docker)
5. Local development setup (step-by-step)
6. Environment variables reference (from `.env.example`)
7. API endpoint summary
8. Running tests
9. Docker deployment instructions
10. Design trade-offs and decisions

---

## Coding Challenge Mode

When time is limited, prioritize in this order:

1. Working backend API with clean data model
2. Flyway migration + proper schema
3. Correct architecture (Controller → Service → Repository)
4. Global error handling
5. Basic service tests
6. Frontend with core views
7. Docker Compose for easy startup
8. README

**Rules:**

- Avoid over-engineering. A complete minimal solution beats an incomplete complex one.
- Use H2 for rapid backend startup if PostgreSQL setup would exceed time constraints.
- Skip caching, security, and CI/CD unless explicitly required.
- If a task exceeds 15 minutes, simplify it.

---

## AI Code Review Checklist

Before committing AI-generated code, verify:

| Area              | Check                                                        |
|-------------------|--------------------------------------------------------------|
| Architecture      | Correct layering (Controller → Service → Repository)         |
| Data Handling     | No manual parsing — libraries used (OpenCSV, POI, Jackson)   |
| Mapping           | MapStruct used — no manual `fromEntity()` methods            |
| Serialization     | Jackson annotations — no manual date/JSON formatting         |
| Validation        | Jakarta annotations on request DTOs, `@Valid` on controllers |
| Transactions      | `@Transactional` on service write methods                    |
| Exceptions        | Global handler exists, no stack trace leaks                  |
| Migrations        | Flyway migration for every schema change                     |
| Testing           | Service unit tests + at least one integration test           |
| Security          | No hardcoded secrets, inputs validated, CORS configured      |
| Performance       | No N+1 queries, pagination on list endpoints                 |
| API Contract      | Lists wrapped in response objects, correct HTTP status codes |
| Documentation     | OpenAPI annotations on controllers                           |
| Dependencies      | All libraries exist, are maintained, and version-compatible  |
| Docker            | Multi-stage build, health checks, non-root user              |

---

## Library Preference Rules (Quick Reference)

| Task                  | Use This                    | Never Do This                            |
|-----------------------|-----------------------------|------------------------------------------|
| CSV parsing           | OpenCSV / Commons CSV       | `line.split(",")`                        |
| Excel files           | Apache POI                  | Manual file I/O                          |
| JSON serialization    | Jackson                     | Manual string concatenation              |
| Object mapping        | MapStruct                   | Manual `toDto()` / `fromEntity()` methods|
| Validation            | Jakarta Validation          | Manual if/else checks in services        |
| Date formatting       | Jackson `@JsonFormat`       | Converting to String in DTOs             |
| Database migration    | Flyway                      | `ddl-auto=update`                        |
| API documentation     | SpringDoc OpenAPI           | Manual Postman collections only          |
| Connection pooling    | HikariCP (Boot default)     | Manual DataSource configuration          |
| Pagination            | Spring `Pageable`           | Manual LIMIT/OFFSET queries              |
| Caching               | Spring Cache + Caffeine/Redis | Manual HashMap caching                 |
| HTTP interception     | Angular HttpInterceptorFn   | Manual token injection per request       |
| State management      | Angular Signals / NgRx      | Manual service-based state with BehaviorSubject spaghetti |
| CSS / Styling         | Tailwind CSS                | Writing custom CSS from scratch, mixing CSS frameworks |
| Alerts / Dialogs      | SweetAlert2 (`@sweetalert2/ngx-sweetalert2`) | Browser-native `alert()` / `confirm()` / `prompt()` |
| Toast notifications   | ngx-toastr                  | Custom toast components, manual DOM injection |
| Loading spinners      | ngx-spinner                 | Custom spinner components, CSS-only hacks |

---

## Final Rule

The AI is a tool assisting the engineer. The human developer remains the final authority.

Every recommendation in this document should be treated as a strong default — not an unbreakable law. If a specific situation calls for a different approach, the AI should explain why it's deviating and get confirmation.
