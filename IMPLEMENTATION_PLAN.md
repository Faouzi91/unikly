# Unikly — Full Implementation Plan

> **Created:** 2026-03-24
> **Scope:** Everything required for a complete, working end-to-end workflow.
> Work items are ordered by dependency: complete each phase before starting the next.

---

## How to read this file

Each item includes:
- **What** — what exactly needs to be done
- **Where** — exact file(s) to change
- **Why** — what breaks without it

Status markers: `[ ]` = todo · `[x]` = done

---

## PHASE 1 — Backend: Re-enable commented endpoints

These endpoints are called by the frontend but currently commented out in the controller, causing 404/405 errors.

---

### 1.1 `GET /api/v1/jobs` — List client's own jobs ✅ DONE
**Status:** Already fixed in this session.
The `listMyJobs()` endpoint was uncommented and wired to `getJobsByClient()`.

---

### 1.2 `GET /api/v1/jobs/my-contracts` — Freelancer's active contracts

**File:** `backend/job-service/src/main/java/com/unikly/jobservice/api/JobController.java`
**File:** `backend/job-service/src/main/java/com/unikly/jobservice/application/service/JobService.java`

**What to do:**

1. Add `getFreelancerContracts(UUID freelancerId, int page, int size)` to `JobService`:
   - Query: `jobRepository.findByFreelancerIdAndStatusIn(freelancerId, List.of(IN_PROGRESS, IN_REVIEW, COMPLETED), pageable)`
   - This requires a new repo method (see 1.2a)
   - Return `Page<JobResponse>`

2. Add repo method to `JobRepository`:
   ```java
   Page<Job> findByAcceptedFreelancerIdAndStatusIn(UUID freelancerId, List<JobStatus> statuses, Pageable pageable);
   ```
   > **Note:** Check the `Job` entity — the accepted freelancer is stored either as a field on `Job` or via the `Proposal` with `status=ACCEPTED`. If it's on `Proposal`, the query needs to be a custom JPQL joining proposals.

3. Uncomment and implement in `JobController`:
   ```java
   @GetMapping("/my-contracts")
   public ResponseEntity<PageResponse<JobResponse>> getMyContracts(
           @RequestParam(defaultValue = "0") int page,
           @RequestParam(defaultValue = "20") int size) {
       UUID freelancerId = UserContext.getUserId();
       var result = jobService.getFreelancerContracts(freelancerId, page, size);
       return ResponseEntity.ok(new PageResponse<>(
               result.getContent(), result.getNumber(), result.getSize(),
               result.getTotalElements(), result.getTotalPages()));
   }
   ```

**Why:** The freelancer "My Contracts" tab calls `GET /api/v1/jobs/my-contracts` and currently gets a 404. Freelancers can't see their active work.

---

### 1.3 `PATCH /api/v1/jobs/{id}/submit-delivery` — Freelancer marks work done

**File:** `backend/job-service/src/main/java/com/unikly/jobservice/api/JobController.java`
**File:** `backend/job-service/src/main/java/com/unikly/jobservice/application/service/JobService.java`

**What to do:**

1. Add `submitDelivery(UUID jobId, UUID freelancerId, String note)` to `JobService`:
   - Load job, verify `job.status == IN_PROGRESS`
   - Verify caller is the accepted freelancer
   - Transition job to `COMPLETED`
   - Publish a `JobCompletedEvent` (or reuse `JobStatusChangedEvent`) via outbox
   - Return updated `JobResponse`

2. Uncomment in `JobController`:
   ```java
   @PatchMapping("/{id}/submit-delivery")
   public ResponseEntity<JobResponse> submitDelivery(
           @PathVariable UUID id,
           @RequestBody(required = false) Map<String, String> body) {
       UUID freelancerId = UserContext.getUserId();
       String note = body != null ? body.getOrDefault("note", "") : "";
       return ResponseEntity.ok(jobService.submitDelivery(id, freelancerId, note));
   }
   ```

3. Add `Map` import back to controller (was removed):
   ```java
   import java.util.Map;
   ```

**Why:** Without this, freelancers have no way to signal work completion. Jobs stay IN_PROGRESS forever.

---

### 1.4 `GET /admin/stats` — Admin dashboard stats (optional, lower priority)

**File:** `backend/job-service/src/main/java/com/unikly/jobservice/api/JobController.java`

**What to do:**

1. Add `getTotalActiveJobs()` to `JobService` if missing:
   ```java
   public long getTotalActiveJobs() {
       return jobRepository.countByStatusIn(List.of(JobStatus.OPEN, JobStatus.IN_REVIEW, JobStatus.IN_PROGRESS));
   }
   ```

2. Uncomment in controller:
   ```java
   @GetMapping("/admin/stats")
   @PreAuthorize("hasRole('ROLE_ADMIN')")
   public ResponseEntity<Map<String, Long>> getAdminStats() {
       return ResponseEntity.ok(Map.of("totalActiveJobs", jobService.getTotalActiveJobs()));
   }
   ```

3. Add back imports: `Map`, `PreAuthorize`, `BigDecimal` (if needed).

**Why:** Admin dashboard calls this endpoint. Fails silently but shows 0 on admin stats.

---

## PHASE 2 — Frontend: Critical workflow fixes

These are blocking the core platform workflow.

---

### 2.1 Job DRAFT → OPEN: Add "Publish" button

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.html`
**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.ts`
**File:** `frontend/unikly-app/src/app/features/jobs/services/job.service.ts`

**What to do:**

1. In `job-detail.component.ts`, add:
   ```typescript
   get canPublish(): boolean {
     return this.isJobOwner && this.job?.status === 'DRAFT';
   }

   publishJob(): void {
     if (!this.job) return;
     this.jobService.updateJobStatus(this.job.id, 'OPEN').subscribe({
       next: (updated) => {
         this.job = updated;
         this.toast.success('Job published. Freelancers can now apply.');
       },
       error: () => this.toast.error('Failed to publish job.'),
     });
   }
   ```

2. In `job-detail.component.html`, add publish button alongside Edit/Cancel buttons:
   ```html
   @if (canPublish) {
     <button type="button"
       class="rounded-lg bg-brand px-3 py-1.5 text-sm font-medium text-white transition hover:bg-brand-dark"
       (click)="publishJob()">
       Publish Job
     </button>
   }
   ```

3. In `job-list.component.html` (Mine tab), add a "Publish" link on DRAFT job cards so clients can publish directly from the list without entering the detail view.

**Why:** Jobs are created as DRAFT but there's no way to move them to OPEN. New jobs are invisible to freelancers.

---

### 2.2 Proposal resubmit: hydrate `myProposal` on load

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.ts`

**What to do:**

In `loadProposals()`, after loading the proposals array, find the current user's proposal and set `myProposal`:

```typescript
private loadProposals(jobId: string): void {
  this.jobService.getProposals(jobId).subscribe({
    next: (response) => {
      this.ownerConfirmedViaApi.set(true);
      this.proposals = response.content;

      // ← ADD THIS: hydrate myProposal from the loaded list
      const currentUserId = this.keycloak.getUserId();
      const mine = this.proposals.find(p => p.freelancerId === currentUserId);
      if (mine) {
        this.myProposal.set(mine);
      }

      // ... rest of existing code (profile loading, tab switching)
    },
    error: (err) => { /* existing */ },
  });
}
```

**Why:** `canResubmit` depends on `myProposal()` being non-null. Currently `myProposal` is only set when a proposal is submitted in the same session. After a page reload, freelancers with `NEEDS_REVIEW`/`OUTDATED` proposals see no resubmit banner.

**Edge case:** `getProposals()` returns 403 for non-owners. When a freelancer loads the page, this call will fail. A separate endpoint is needed — see item 2.2a.

---

### 2.2a Backend: Add `GET /api/v1/jobs/{id}/proposals/mine` — freelancer's own proposal

**File:** `backend/job-service/src/main/java/com/unikly/jobservice/api/ProposalController.java` (or wherever proposals are routed)

**What to do:**

1. Add endpoint that returns only the caller's proposal for a given job:
   ```java
   @GetMapping("/{jobId}/proposals/mine")
   public ResponseEntity<ProposalResponse> getMyProposal(@PathVariable UUID jobId) {
       UUID freelancerId = UserContext.getUserId();
       return proposalService.getProposalByFreelancerAndJob(freelancerId, jobId)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
   }
   ```

2. In `ProposalService`, add:
   ```java
   public Optional<ProposalResponse> getProposalByFreelancerAndJob(UUID freelancerId, UUID jobId) {
       return proposalRepository.findByFreelancerIdAndJobId(freelancerId, jobId)
               .map(proposalMapper::toResponse);
   }
   ```

3. Add repo method: `Optional<Proposal> findByFreelancerIdAndJobId(UUID freelancerId, UUID jobId);`

4. In `job.service.ts`, add:
   ```typescript
   getMyProposal(jobId: string): Observable<Proposal> {
     return this.api.get<Proposal>(`/v1/jobs/${jobId}/proposals/mine`);
   }
   ```

5. In `job-detail.component.ts`, call `getMyProposalForFreelancer()` during `ngOnInit` if the user is a freelancer:
   ```typescript
   ngOnInit(): void {
     const jobId = this.route.snapshot.paramMap.get('id');
     if (jobId) {
       this.loadJob(jobId);
       if (this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER')) {
         this.jobService.getMyProposal(jobId).subscribe({
           next: (p) => this.myProposal.set(p),
           error: () => { /* 404 = no proposal yet, ignore */ },
         });
       }
     }
   }
   ```

**Why:** Freelancers cannot currently see their own proposal status on page load (proposals endpoint returns 403 to non-owners).

---

### 2.3 After payment: reload job + auto-transition display

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.ts`

**What to do:**

In `onPaymentCompleted()`, after reloading payment, also reload the full job to reflect the status change from `IN_REVIEW` → `IN_PROGRESS` (which happens via the `PaymentCompletedEvent` Kafka consumer on the backend):

```typescript
onPaymentCompleted(): void {
  this.showPaymentModal.set(false);
  if (this.job) {
    this.loadJob(this.job.id);   // reloads job + proposals + payment in one call
  }
}
```

Currently `onPaymentCompleted()` only calls `loadPayment()`, so the lifecycle stepper stays on "Awaiting Payment" even after the backend has moved the job to IN_PROGRESS.

**Note:** The backend transition via Kafka is async (not instant). Add a 1.5s delay before reloading, or poll until status changes:

```typescript
onPaymentCompleted(): void {
  this.showPaymentModal.set(false);
  if (this.job) {
    // Small delay to allow Kafka consumer to process PaymentCompletedEvent
    setTimeout(() => this.loadJob(this.job!.id), 2000);
  }
}
```

**Why:** After funding escrow, the job status visually remains "Awaiting Payment" even though the backend has moved it to IN_PROGRESS.

---

### 2.4 Freelancer: "Mark as Complete" button

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.ts`
**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.html`
**File:** `frontend/unikly-app/src/app/features/jobs/services/job.service.ts`

**What to do:**

1. Add `submitDelivery(id: string, note?: string)` to `job.service.ts`:
   ```typescript
   submitDelivery(jobId: string, note = ''): Observable<Job> {
     return this.api.patch<Job>(`/v1/jobs/${jobId}/submit-delivery`, { note });
   }
   ```

2. Add getter and method to `job-detail.component.ts`:
   ```typescript
   get canSubmitDelivery(): boolean {
     const userId = this.keycloak.getUserId();
     return (
       this.job?.status === 'IN_PROGRESS' &&
       (this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER')) &&
       this.acceptedProposal?.freelancerId === userId
     );
   }

   async confirmSubmitDelivery(): Promise<void> {
     if (!this.job) return;
     const result = await Swal.fire({
       title: 'Submit work for delivery?',
       text: 'This will mark the project as completed. The client will be able to release payment.',
       icon: 'question',
       showCancelButton: true,
       confirmButtonText: 'Submit Delivery',
       confirmButtonColor: '#14a800',
     });
     if (!result.isConfirmed) return;
     this.jobService.submitDelivery(this.job.id).subscribe({
       next: (updated) => {
         this.job = updated;
         this.toast.success('Work submitted. Awaiting client confirmation.');
       },
       error: () => this.toast.error('Failed to submit delivery.'),
     });
   }
   ```

3. In `job-detail.component.html`, add inside the `@if (job)` block (near payment section, visible to freelancer):
   ```html
   @if (canSubmitDelivery) {
     <div class="card-panel flex flex-wrap items-center justify-between gap-4 border border-sky-200 bg-sky-50 p-4">
       <div>
         <p class="text-sm font-semibold text-sky-800">Work in progress</p>
         <p class="text-xs text-sky-700">When you've completed the work, submit it for client review.</p>
       </div>
       <button type="button" class="btn-primary" (click)="confirmSubmitDelivery()">
         Submit Delivery
       </button>
     </div>
   }
   ```

**Why:** Freelancers have no way to signal completion. The job stays IN_PROGRESS indefinitely.

---

### 2.5 After job edit: refresh proposals list

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.ts`

**What to do:**

In `saveJobEdit()`, after the `next: (updated)` callback, reload proposals so their updated statuses (OUTDATED/NEEDS_REVIEW) appear:

```typescript
next: (updated) => {
  this.job = updated;
  this.editMode.set(false);
  this.editSaving.set(false);
  if (confirmed) {
    this.loadProposals(updated.id);   // ← ADD THIS
  }
  const msg = confirmed
    ? 'Job updated. Affected proposals have been notified.'
    : 'Job updated successfully.';
  this.toast.success(msg);
},
```

**Why:** After a client edits a job, proposal cards still show `SUBMITTED` status. The backend has already marked them `OUTDATED`/`NEEDS_REVIEW` but the list isn't refreshed.

---

## PHASE 3 — Frontend: Messaging entry points

Messaging exists but there's no way to start or navigate to a conversation from anywhere in the app.

---

### 3.1 "Message" button on proposal cards (client → freelancer)

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.html`
**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.ts`

**What to do:**

1. Inject `MessagingService` and `Router` (already injected) in `job-detail.component.ts`.

2. Add method:
   ```typescript
   openConversation(otherUserId: string): void {
     const currentUserId = this.keycloak.getUserId()!;
     this.messagingService.getOrCreateConversation([currentUserId, otherUserId])
       .subscribe({
         next: (conv) => this.router.navigate(['/messages', conv.id]),
         error: () => this.toast.error('Could not open conversation.'),
       });
   }
   ```

3. In `job-detail.component.html`, add a "Message" button inside each proposal card's action area (alongside Accept/Decline):
   ```html
   <button type="button"
     class="rounded-lg border border-ink-200 bg-white px-3 py-2 text-sm font-medium text-ink-700 hover:bg-ink-50"
     (click)="openConversation(proposal.freelancerId)">
     Message
   </button>
   ```

4. Also add a "Message Freelancer" button in the accepted proposal section (when `proposal.status === 'ACCEPTED'`).

---

### 3.2 "Message" button on freelancer search results

**File:** `frontend/unikly-app/src/app/features/search/search/search.component.ts`
**File:** `frontend/unikly-app/src/app/features/search/search/search.component.html`

**What to do:**

1. Inject `MessagingService` and `Router` in `search.component.ts`.

2. Add `openConversation(freelancerId: string)` method (same logic as 3.1).

3. In the freelancer search results card template, add a "Message" button:
   ```html
   <button type="button" (click)="openConversation(result.userId)"
     class="btn-secondary text-sm">
     Message
   </button>
   ```

---

### 3.3 "Message" button on public freelancer profile page

**File:** `frontend/unikly-app/src/app/features/profile/public-profile/` (check what exists)

**What to do:**

1. Find the public profile component (`/users/:id` route).
2. Add `openConversation()` method and a "Message" button in the profile header, visible only to logged-in users who are not viewing their own profile.

---

### 3.4 MessagingService: add `getOrCreateConversation` if missing

**File:** `frontend/unikly-app/src/app/features/messaging/services/messaging.service.ts`

**What to do:**

Check that `getOrCreateConversation(participantIds: string[], jobId?: string)` exists and calls `POST /api/v1/messages/conversations`. If missing, add:

```typescript
getOrCreateConversation(participantIds: string[], jobId?: string): Observable<Conversation> {
  return this.api.post<Conversation>('/v1/messages/conversations', { participantIds, jobId });
}
```

---

## PHASE 4 — Frontend: Notification completeness

---

### 4.1 Add missing notification type labels

**File:** `frontend/unikly-app/src/app/features/notifications/notification-list/notification-list.component.ts`

**What to do:**

Extend `TYPE_LABELS` to cover all types the backend sends:

```typescript
const TYPE_LABELS: Record<string, string> = {
  JOB_MATCHED:              'Matched',
  PROPOSAL_RECEIVED:        'Proposal',
  PROPOSAL_ACCEPTED:        'Accepted',
  PROPOSAL_REJECTED:        'Rejected',
  PROPOSAL_OUTDATED:        'Update Required',   // ← ADD
  JOB_UPDATED:              'Job Updated',        // ← ADD
  JOB_CANCELLED:            'Job Cancelled',      // ← ADD
  JOB_COMPLETED:            'Completed',          // ← ADD
  PAYMENT_FUNDED:           'Escrow Funded',
  ESCROW_RELEASED:          'Payment Released',
  MESSAGE_RECEIVED:         'Message',
  REVIEW_RECEIVED:          'Review',             // ← ADD
  SYSTEM:                   'System',
};
```

Check the notification-service `NotificationType` enum for the complete list and sync with it.

---

### 4.2 Real-time unread badge on notification bell

**File:** `frontend/unikly-app/src/app/shared/components/notification-bell/notification-bell.component.ts`

**What to do:**

1. Check if `NotificationService` has a WebSocket/SSE subscription for real-time updates.
2. If not, add polling: in `NotificationService.init()`, set up an interval (e.g., every 60s) to refresh the unread count.
3. Ensure the `unreadCount` signal in `NotificationService` is updated when new notifications arrive, and that the bell badge reads from it.

The `MainLayoutComponent` already computes `messageUnreadCount` from `notificationService.notifications()`. Verify the bell component also displays the total unread count (not just message unread).

---

## PHASE 5 — Frontend: Review system completeness

---

### 5.1 Freelancer can review the client

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.ts`
**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.html`

**What to do:**

1. Add getter:
   ```typescript
   get canFreelancerLeaveReview(): boolean {
     const userId = this.keycloak.getUserId();
     return (
       this.job?.status === 'COMPLETED' &&
       this.acceptedProposal?.freelancerId === userId &&
       !this.isJobOwner
     );
   }
   ```

2. Add `reviewClientDialogData` getter that sets `revieweeId = job.clientId`.

3. In the template, add a "Leave Review" panel for the freelancer (mirroring the client's review panel, but bound to `canFreelancerLeaveReview`).

**Why:** Review system is currently one-way. Clients can't be reviewed.

---

### 5.2 Prevent duplicate reviews

**Backend:** `backend/user-service/src/main/java/com/unikly/userservice/application/ReviewService.java`

**What to do:**

Check if `ReviewService` enforces one review per (reviewer, reviewee, jobId) combination. If not, add a unique constraint in the migration and a check in the service. Return a meaningful error message if a duplicate is attempted.

---

## PHASE 6 — Frontend: Polish & error handling

---

### 6.1 Show error toasts on failed API calls

**Files:** All component `.ts` files

**What to do:**

Systematically audit every `error: () => this.loading = false` and replace with:
```typescript
error: (err) => {
  this.loading = false;
  this.toast.error('Something went wrong. Please try again.');
}
```

Priority components to fix:
- `job-list.component.ts` — `loadJobs()` and `loadMyJobs()` error handlers
- `job-detail.component.ts` — `loadProposals()`, `loadMatches()`, `loadPayment()`, `acceptProposal()`
- `payment-status.component.ts` — `onRelease()`, `onRefund()`

---

### 6.2 Job list: show DRAFT jobs with "Publish" CTA

**File:** `frontend/unikly-app/src/app/features/jobs/job-list/job-list.component.html`

**What to do:**

In the "My Projects" (mine tab) card list, detect when `job.status === 'DRAFT'` and show a prominent "Publish" button instead of just a status badge:
```html
@if (job.status === 'DRAFT') {
  <a [routerLink]="['/jobs', job.id]" class="text-xs font-semibold text-brand-dark hover:underline">
    Open to Publish →
  </a>
}
```

---

### 6.3 Dashboard: show draft job count for clients

**File:** `frontend/unikly-app/src/app/features/dashboard/dashboard/dashboard.ts`

**What to do:**

When loading `myJobs` for the dashboard, also count how many are DRAFT and show a call-to-action if any exist:
```
You have 2 unpublished job drafts. Publish them so freelancers can apply.
```

---

### 6.4 Search: freelancer results link to public profile

**File:** `frontend/unikly-app/src/app/features/search/search/search.component.html`

**What to do:**

Ensure each freelancer search result card has:
- A link to `/users/:id` (public profile)
- A "Message" button (from Phase 3.2)

---

### 6.5 Matching tab: "Invite to Apply" button for clients

**File:** `frontend/unikly-app/src/app/features/jobs/job-detail/job-detail.component.html`

**What to do:**

In the Matches tab, add an "Invite" button on each match card that opens a conversation with the matched freelancer (reuse `openConversation()` from Phase 3.1):

```html
<button type="button"
  class="btn-secondary text-sm"
  (click)="openConversation(match.freelancerId)">
  Invite to Apply
</button>
```

---

## PHASE 7 — Integration checks

After all above items are implemented, verify the following end-to-end flows manually:

---

### Flow A: Full job lifecycle (Happy Path)
1. **[CLIENT]** Create job → lands in DRAFT
2. **[CLIENT]** Click "Publish" → status becomes OPEN
3. **[FREELANCER]** Browse jobs → find the job → click "Submit Proposal"
4. **[CLIENT]** Open job detail → Proposals tab auto-selected → see freelancer's proposal
5. **[CLIENT]** Click "✓ Hire" → accept confirmation → job moves to IN_REVIEW, proposal shows ACCEPTED
6. **[CLIENT]** Fund Escrow banner appears → click "Fund Escrow" → Stripe payment dialog → pay
7. **[SYSTEM]** After ~2s, job status moves to IN_PROGRESS (Kafka consumer processed PaymentCompletedEvent)
8. **[FREELANCER]** Open My Contracts → job appears → open detail → "Submit Delivery" button visible
9. **[FREELANCER]** Click "Submit Delivery" → confirm → job moves to COMPLETED
10. **[CLIENT]** Open job detail → "Leave a Review" button visible → review submitted
11. **[FREELANCER]** Open job detail → "Leave a Review" button visible → reviews the client
12. **[CLIENT]** Payment status shows "Escrow Funded" → click "Release" → payment released to freelancer

---

### Flow B: Edit job after proposals received
1. **[CLIENT]** Job is OPEN with 2 SUBMITTED proposals
2. **[CLIENT]** Edit job → change budget and skills → Save
3. Confirmation dialog shows "2 active proposal(s) affected" → confirm
4. **[CLIENT]** Proposals tab reloads → both proposals show NEEDS_REVIEW/OUTDATED status
5. **[FREELANCER A]** Opens the job → resubmit banner visible (even after page reload) → clicks "Review & Resubmit"
6. **[FREELANCER A]** Resubmits with updated budget → proposal back to SUBMITTED
7. **[CLIENT]** Proposal card shows updated status

---

### Flow C: Messaging flow
1. **[CLIENT]** Open job proposals → click "Message" on a freelancer
2. Redirects to `/messages/:conversationId` (conversation created or existing one opened)
3. **[FREELANCER]** Navigates to Messages → sees conversation from client
4. Both sides can send messages

---

### Flow D: Cancel job
1. **[CLIENT]** Job is OPEN with 1 SUBMITTED proposal
2. **[CLIENT]** Click "Cancel Job" → confirmation → job status → CANCELLED
3. Redirected to `/jobs`
4. **[FREELANCER]** Gets a notification "Job Cancelled"

---

## Summary Table

| # | Area | Item | Phase |
|---|------|------|-------|
| 1.1 | Backend | `GET /api/v1/jobs` | ✅ Done |
| 1.2 | Backend | `GET /api/v1/jobs/my-contracts` | 1 |
| 1.3 | Backend | `PATCH /api/v1/jobs/{id}/submit-delivery` | 1 |
| 1.4 | Backend | `GET /admin/stats` | 1 (low) |
| 2.1 | Frontend | Publish DRAFT job | 2 |
| 2.2 | Frontend | Hydrate `myProposal` on load | 2 |
| 2.2a | Backend + Frontend | `GET /proposals/mine` endpoint | 2 |
| 2.3 | Frontend | Reload job after payment | 2 |
| 2.4 | Frontend + Backend | Freelancer "Submit Delivery" | 2 |
| 2.5 | Frontend | Reload proposals after job edit | 2 |
| 3.1 | Frontend | Message button on proposal cards | 3 |
| 3.2 | Frontend | Message button on freelancer search | 3 |
| 3.3 | Frontend | Message button on public profile | 3 |
| 3.4 | Frontend | `getOrCreateConversation` in service | 3 |
| 4.1 | Frontend | All notification type labels | 4 |
| 4.2 | Frontend | Real-time notification badge | 4 |
| 5.1 | Frontend | Freelancer reviews client | 5 |
| 5.2 | Backend | Prevent duplicate reviews | 5 |
| 6.1 | Frontend | Error toasts on API failures | 6 |
| 6.2 | Frontend | DRAFT jobs with Publish CTA in list | 6 |
| 6.3 | Frontend | Dashboard draft job count | 6 |
| 6.4 | Frontend | Search freelancer links | 6 |
| 6.5 | Frontend | Matches "Invite to Apply" button | 6 |

---

*Total: 5 backend items · 16 frontend items · 4 integration flows*
