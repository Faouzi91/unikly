# Unikly — API Reference

All endpoints are accessed through the **API Gateway** at `http://localhost:8080`.

Every authenticated request requires a **Bearer JWT** in the `Authorization` header (obtained from Keycloak). The gateway validates the token and relays `X-User-Id` and `X-User-Roles` headers to downstream services.

Interactive Swagger UI: <http://localhost:8080/swagger-ui.html>

---

## Table of Contents

1. [Job Service](#job-service)
2. [User Service](#user-service)
3. [Payment Service](#payment-service)
4. [Matching Service](#matching-service)
5. [Messaging Service](#messaging-service)
6. [Notification Service](#notification-service)
7. [Search Service](#search-service)
8. [Stripe Webhooks](#stripe-webhooks)
9. [Common Error Response](#common-error-response)

---

## Job Service

Base path: `/api/v1/jobs`

### Create a Job

```
POST /api/v1/jobs
Authorization: Bearer <token>
Role required: ROLE_CLIENT
```

**Request body:**

```json
{
  "title": "Build a REST API",
  "description": "Need a Spring Boot developer to build a REST API with PostgreSQL.",
  "budgetAmount": 2500.00,
  "budgetCurrency": "USD",
  "skills": ["Java", "Spring Boot", "PostgreSQL"]
}
```

**Response — 201 Created:**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "title": "Build a REST API",
  "description": "Need a Spring Boot developer to build a REST API with PostgreSQL.",
  "status": "DRAFT",
  "budgetAmount": 2500.00,
  "budgetCurrency": "USD",
  "skills": ["Java", "Spring Boot", "PostgreSQL"],
  "clientId": "user-uuid",
  "proposalCount": 0,
  "createdAt": "2026-03-19T10:00:00Z"
}
```

---

### List Jobs

```
GET /api/v1/jobs?status=OPEN&skill=Java&minBudget=500&maxBudget=5000&sort=createdAt,desc&page=0&size=20
Authorization: Bearer <token>
```

**Response — 200 OK:**

```json
{
  "content": [ { ...job } ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

---

### Get Job Detail

```
GET /api/v1/jobs/{id}
Authorization: Bearer <token>
```

**Response — 200 OK:** Full job object including `proposalCount`.

---

### Update Job (DRAFT only)

```
PATCH /api/v1/jobs/{id}
Authorization: Bearer <token>
Role required: job owner (ROLE_CLIENT)
```

**Request body:** Partial fields (title, description, budgetAmount, budgetCurrency, skills).

---

### Transition Job Status

```
PATCH /api/v1/jobs/{id}/status
Authorization: Bearer <token>
```

**Request body:**

```json
{ "status": "OPEN" }
```

Valid transitions: `DRAFT → OPEN`, `OPEN → CANCELLED`, `IN_PROGRESS → COMPLETED`, etc.
**409 Conflict** on invalid transition.

---

### Submit Proposal

```
POST /api/v1/jobs/{id}/proposals
Authorization: Bearer <token>
Role required: ROLE_FREELANCER
```

**Request body:**

```json
{
  "proposedBudget": 2200.00,
  "coverLetter": "I have 5 years of Spring Boot experience and can deliver within 2 weeks."
}
```

**Response — 201 Created:**

```json
{
  "id": "proposal-uuid",
  "jobId": "job-uuid",
  "freelancerId": "user-uuid",
  "proposedBudget": 2200.00,
  "coverLetter": "...",
  "status": "PENDING",
  "createdAt": "2026-03-19T10:05:00Z"
}
```

---

### List Proposals (job owner only)

```
GET /api/v1/jobs/{id}/proposals
Authorization: Bearer <token>
```

---

### Accept Proposal

```
PATCH /api/v1/jobs/{id}/proposals/{proposalId}/accept
Authorization: Bearer <token>
Role required: job owner
```

Accepts the proposal, rejects all other PENDING proposals, creates a Contract, and transitions job to IN_PROGRESS.

---

### Reject Proposal

```
PATCH /api/v1/jobs/{id}/proposals/{proposalId}/reject
Authorization: Bearer <token>
Role required: job owner
```

---

## User Service

Base path: `/api/v1/users`

### Get My Profile

```
GET /api/v1/users/me
Authorization: Bearer <token>
```

**Response — 200 OK:**

```json
{
  "id": "user-uuid",
  "displayName": "Alice Martin",
  "bio": "Full-stack developer with 8 years of experience.",
  "role": "FREELANCER",
  "skills": ["Angular", "Java", "PostgreSQL"],
  "hourlyRate": 75.00,
  "currency": "USD",
  "location": "Paris, France",
  "portfolioLinks": ["https://github.com/alice"],
  "averageRating": 4.8,
  "totalReviews": 23,
  "createdAt": "2025-01-15T09:00:00Z"
}
```

---

### Update My Profile

```
PUT /api/v1/users/me
Authorization: Bearer <token>
```

**Request body:** All profile fields (displayName, bio, hourlyRate, currency, location, skills, portfolioLinks).

---

### Get Public Profile

```
GET /api/v1/users/{id}
Authorization: Bearer <token>
```

---

### Search Freelancers

```
GET /api/v1/users?role=FREELANCER&skill=Angular&page=0&size=20
Authorization: Bearer <token>
```

---

### Submit a Review

```
POST /api/v1/users/{id}/reviews
Authorization: Bearer <token>
```

**Request body:**

```json
{
  "jobId": "job-uuid",
  "rating": 5,
  "comment": "Excellent work, delivered on time!"
}
```

**Response — 201 Created:** Review object.

---

### List Reviews

```
GET /api/v1/users/{id}/reviews?page=0&size=10
Authorization: Bearer <token>
```

---

## Payment Service

Base path: `/api/v1/payments`

### Create Payment Intent

```
POST /api/v1/payments
Authorization: Bearer <token>
Role required: ROLE_CLIENT
```

**Request body:**

```json
{
  "jobId": "job-uuid",
  "freelancerId": "freelancer-uuid",
  "amount": 2200.00,
  "currency": "USD",
  "idempotencyKey": "unique-client-generated-key"
}
```

**Response — 201 Created:**

```json
{
  "paymentId": "payment-uuid",
  "clientSecret": "pi_xxx_secret_yyy"
}
```

Use `clientSecret` with `stripe.js` `confirmPayment()` on the frontend.

---

### List Payments for a Job

```
GET /api/v1/payments?jobId={uuid}
Authorization: Bearer <token>
```

---

### Release Escrow

```
POST /api/v1/payments/{id}/release
Authorization: Bearer <token>
Role required: job client
```

Transitions payment from FUNDED → COMPLETED and publishes `EscrowReleasedEvent`.

---

### Request Refund

```
POST /api/v1/payments/{id}/refund
Authorization: Bearer <token>
Role required: job client
```

Only allowed when status is FUNDED. Calls Stripe `Refund.create`.

---

## Matching Service

Base path: `/api/v1/matches`

### Get Matches for a Job

```
GET /api/v1/matches?jobId={uuid}&limit=20
Authorization: Bearer <token>
```

**Response — 200 OK:**

```json
[
  {
    "id": "match-uuid",
    "jobId": "job-uuid",
    "freelancerId": "user-uuid",
    "score": 0.8765,
    "matchedSkills": ["Java", "Spring Boot"],
    "strategy": "AI_EMBEDDING",
    "createdAt": "2026-03-19T10:30:00Z"
  }
]
```

Score is in [0.0, 1.0]. Strategy is `RULE_BASED` or `AI_EMBEDDING`.

---

### Get Matches for a Freelancer

```
GET /api/v1/matches?freelancerId={uuid}&limit=10
Authorization: Bearer <token>
```

---

### Get Match by ID

```
GET /api/v1/matches/{id}
Authorization: Bearer <token>
```

---

## Messaging Service

Base path: `/api/v1/conversations`

### Get or Create Conversation

```
POST /api/v1/conversations
Authorization: Bearer <token>
```

**Request body:**

```json
{
  "recipientId": "other-user-uuid",
  "jobId": "job-uuid"
}
```

**Response — 200 OK or 201 Created:** Conversation object.

---

### List My Conversations

```
GET /api/v1/conversations?page=0&size=20
Authorization: Bearer <token>
```

Sorted by `lastMessageAt DESC`.

---

### Get Messages in Conversation

```
GET /api/v1/conversations/{id}/messages?page=0&size=50
Authorization: Bearer <token>
```

---

### Send a Message

```
POST /api/v1/conversations/{id}/messages
Authorization: Bearer <token>
```

**Request body:**

```json
{
  "content": "Hello, I'm interested in your job posting.",
  "contentType": "TEXT"
}
```

**Response — 201 Created:** Message object including `id`, `senderId`, `content`, `sentAt`.

---

### WebSocket — Real-Time Messages

Connect to `ws://localhost:8080/ws/messages/websocket` (SockJS).

STOMP subscribe: `/user/{userId}/queue/messages`
STOMP publish (typing): `/app/typing` with body `{ conversationId, recipientId }`

JWT must be passed as the STOMP `Authorization` header on CONNECT.

---

## Notification Service

Base path: `/api/v1/notifications`

### List Notifications

```
GET /api/v1/notifications?unread=false&page=0&size=20
Authorization: Bearer <token>
```

---

### Mark Notification as Read

```
PATCH /api/v1/notifications/{id}/read
Authorization: Bearer <token>
```

---

### Mark All as Read

```
PATCH /api/v1/notifications/read-all
Authorization: Bearer <token>
```

---

### Get Preferences

```
GET /api/v1/notifications/preferences
Authorization: Bearer <token>
```

**Response:**

```json
{
  "emailEnabled": true,
  "pushEnabled": false,
  "realtimeEnabled": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00"
}
```

---

### Save Preferences

```
PUT /api/v1/notifications/preferences
Authorization: Bearer <token>
```

---

### WebSocket — Real-Time Notifications

Connect to `ws://localhost:8080/ws/notifications/websocket` (SockJS).

STOMP subscribe: `/user/{userId}/queue/notifications`

JWT must be passed as the STOMP `Authorization` header on CONNECT.

---

## Search Service

Base path: `/api/v1/search`

### Search Jobs

```
GET /api/v1/search/jobs?q=spring+boot&skills=Java,PostgreSQL&minBudget=500&maxBudget=5000&page=0&size=20
Authorization: Bearer <token>
```

**Response — 200 OK:**

```json
{
  "content": [
    {
      "jobId": "job-uuid",
      "title": "Build a REST API",
      "description": "...",
      "skills": ["Java", "Spring Boot"],
      "budgetAmount": 2500.00,
      "budgetCurrency": "USD",
      "status": "OPEN",
      "score": 1.25
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 8
}
```

---

### Search Freelancers

```
GET /api/v1/search/freelancers?q=angular&skills=Angular,TypeScript&minRating=4.0&page=0&size=20
Authorization: Bearer <token>
```

---

### Get Autocomplete Suggestions

```
GET /api/v1/search/suggestions?q=ang&type=skill
Authorization: Bearer <token>
```

**Response:**

```json
["Angular", "Angular Material", "Angular Universal"]
```

---

## Stripe Webhooks

```
POST /webhooks/stripe
Stripe-Signature: t=1234,v1=abc...
Content-Type: text/plain
```

No JWT required. Signature is verified using `STRIPE_WEBHOOK_SECRET` (HMAC SHA-256).

**Handled events:**

| Event | Action |
|---|---|
| `payment_intent.succeeded` | Payment → FUNDED, create LedgerEntry, publish PaymentCompletedEvent |
| `payment_intent.payment_failed` | Payment → FAILED |

All other event types are acknowledged (200) but ignored.

---

## Common Error Response

All services return a consistent error body:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "traceId": "6b2f1a3c4d5e6f7a",
  "timestamp": "2026-03-19T10:00:00Z",
  "details": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

| HTTP Status | Meaning |
|---|---|
| 400 | Validation error — check `details` array |
| 401 | Missing or expired JWT |
| 403 | Insufficient role or resource ownership |
| 404 | Resource not found |
| 409 | Conflict — duplicate idempotency key, invalid state transition, or optimistic lock failure |
| 422 | Business rule violation (e.g. self-review) |
| 503 | Downstream service unavailable (circuit breaker open) |
