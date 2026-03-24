import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, NgClass } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import Swal from 'sweetalert2';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { ToastService } from '../../../core/services/toast.service';
import { PaymentRecord, PaymentService } from '../../../core/services/payment.service';
import { PaymentDialogComponent, PaymentDialogData } from '../../payments/payment-dialog/payment-dialog.component';
import { PaymentStatusComponent } from '../../payments/payment-status/payment-status.component';
import { EditDecision, Job, MatchEntry, Proposal, SubmitProposalRequest, UpdateJobRequest } from '../models/job.models';
import { ProposalDialogComponent, ProposalDialogData } from '../components/proposal-dialog/proposal-dialog.component';
import { ReviewDialog, ReviewDialogData } from '../components/review-dialog/review-dialog';
import { UserService } from '../../profile/services/user.service';
import { ReviewRequest, UserProfile } from '../../profile/models/user.models';
import { JobService } from '../services/job.service';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [
    CommonModule,
    NgClass,
    RouterLink,
    FormsModule,
    TimeAgoPipe,
    ProposalDialogComponent,
    PaymentDialogComponent,
    PaymentStatusComponent,
    ReviewDialog,
  ],
  templateUrl: './job-detail.component.html',
  styleUrl: './job-detail.component.scss',
})
export class JobDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly jobService = inject(JobService);
  private readonly keycloak = inject(KeycloakService);
  private readonly paymentService = inject(PaymentService);
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);

  job: Job | null = null;
  proposals: Proposal[] = [];
  matches: MatchEntry[] = [];
  payment: PaymentRecord | null = null;
  loading = true;
  matchesLoading = false;
  readonly ownerConfirmedViaApi = signal(false);
  readonly freelancerProfiles = signal<Record<string, UserProfile>>({});

  readonly activeTab = signal<'details' | 'proposals' | 'matches'>('details');
  readonly showProposalModal = signal(false);
  readonly proposalSubmitting = signal(false);
  readonly showPaymentModal = signal(false);
  readonly showReviewModal = signal(false);
  readonly reviewSubmitting = signal(false);

  // Edit mode
  readonly editMode = signal(false);
  readonly editTitle = signal('');
  readonly editDescription = signal('');
  readonly editBudget = signal(0);
  readonly editSkillsRaw = signal('');
  readonly editSaving = signal(false);

  // Freelancer resubmit — tracks the freelancer's own proposal within the session
  readonly myProposal = signal<Proposal | null>(null);
  readonly showResubmitModal = signal(false);
  readonly resubmitting = signal(false);

  get isJobOwner(): boolean {
    return (
      this.job?.clientId === this.keycloak.getUserId() ||
      this.ownerConfirmedViaApi()
    );
  }

  get hasAcceptedProposal(): boolean {
    return this.proposals.some((proposal) => proposal.status === 'ACCEPTED');
  }

  get acceptedProposal(): Proposal | undefined {
    return this.proposals.find((proposal) => proposal.status === 'ACCEPTED');
  }

  get pendingProposalCount(): number {
    return this.proposals.filter(
      (p) =>
        p.status === 'SUBMITTED' ||
        p.status === 'PENDING' ||
        p.status === 'VIEWED' ||
        p.status === 'SHORTLISTED',
    ).length;
  }

  get sortedProposals(): Proposal[] {
    const order: Record<string, number> = {
      ACCEPTED: 0,
      SHORTLISTED: 1, SUBMITTED: 2, PENDING: 3, VIEWED: 4,
      NEEDS_REVIEW: 5, OUTDATED: 6,
      REJECTED: 7, WITHDRAWN: 8,
    };
    return [...this.proposals].sort((a, b) => (order[a.status] ?? 9) - (order[b.status] ?? 9));
  }

  isActionableProposal(status: string): boolean {
    return (
      status === 'SUBMITTED' ||
      status === 'PENDING' ||
      status === 'VIEWED' ||
      status === 'SHORTLISTED'
    );
  }

  freelancerInitials(name: string): string {
    return name.trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase() || '?';
  }

  starsArray(rating: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < Math.round(rating));
  }

  get canLeaveReview(): boolean {
    return (
      this.job?.status === 'COMPLETED' &&
      this.isJobOwner &&
      !!this.acceptedProposal
    );
  }

  get reviewDialogData(): ReviewDialogData {
    const accepted = this.acceptedProposal;
    return {
      jobId: this.job?.id ?? '',
      jobTitle: this.job?.title ?? '',
      revieweeId: accepted?.freelancerId ?? '',
      revieweeName: accepted?.freelancerName || 'Freelancer',
    };
  }

  get showFundEscrow(): boolean {
    return (
      this.isJobOwner &&
      this.hasAcceptedProposal &&
      this.payment === null &&
      (this.job?.status === 'IN_REVIEW' ||
       this.job?.status === 'IN_PROGRESS' ||
       this.job?.status === 'OPEN')
    );
  }

  get canSubmitProposal(): boolean {
    return (
      this.job?.status === 'OPEN' &&
      (this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER')) &&
      !this.isJobOwner
    );
  }

  get canEdit(): boolean {
    return (
      this.isJobOwner &&
      (this.job?.status === 'DRAFT' || this.job?.status === 'OPEN')
    );
  }

  get canCancel(): boolean {
    return (
      this.isJobOwner &&
      (this.job?.status === 'DRAFT' || this.job?.status === 'OPEN')
    );
  }

  /** True when the freelancer's own proposal is stale and needs resubmission */
  get canResubmit(): boolean {
    const p = this.myProposal();
    return !!p && (p.status === 'OUTDATED' || p.status === 'NEEDS_REVIEW');
  }

  get resubmitDialogData(): ProposalDialogData {
    const p = this.myProposal();
    return {
      jobTitle: this.job?.title ?? '',
      jobBudget: this.job?.budget ?? 0,
      jobCurrency: this.job?.currency ?? 'USD',
      isResubmit: true,
      existingBudget: p?.proposedBudget,
      existingCoverLetter: p?.coverLetter,
    };
  }

  get proposalDialogData(): ProposalDialogData {
    return {
      jobTitle: this.job?.title ?? '',
      jobBudget: this.job?.budget ?? 0,
      jobCurrency: this.job?.currency ?? 'USD',
    };
  }

  get paymentDialogData(): PaymentDialogData {
    const accepted = this.acceptedProposal;
    return {
      jobId: this.job?.id ?? '',
      jobTitle: this.job?.title ?? '',
      budget: accepted?.proposedBudget ?? 0,
      currency: this.job?.currency ?? 'USD',
      freelancerId: accepted?.freelancerId ?? '',
    };
  }

  ngOnInit(): void {
    const jobId = this.route.snapshot.paramMap.get('id');
    if (jobId) {
      this.loadJob(jobId);
    } else {
      this.loading = false;
    }
  }

  openProposalDialog(): void {
    this.showProposalModal.set(true);
  }

  closeProposalDialog(): void {
    this.showProposalModal.set(false);
  }

  submitProposal(payload: SubmitProposalRequest): void {
    if (!this.job) return;
    this.proposalSubmitting.set(true);
    this.jobService.submitProposal(this.job.id, payload).subscribe({
      next: (proposal) => {
        this.myProposal.set(proposal);
        this.proposalSubmitting.set(false);
        this.showProposalModal.set(false);
        this.toast.success('Proposal submitted successfully.');
        this.loadJob(this.job!.id);
      },
      error: () => this.proposalSubmitting.set(false),
    });
  }

  resubmitProposalSubmit(payload: SubmitProposalRequest): void {
    const p = this.myProposal();
    if (!this.job || !p) return;
    this.resubmitting.set(true);
    this.jobService.resubmitProposal(this.job.id, p.id, payload).subscribe({
      next: (updated) => {
        this.myProposal.set(updated);
        this.resubmitting.set(false);
        this.showResubmitModal.set(false);
        this.toast.success('Proposal resubmitted successfully.');
      },
      error: () => this.resubmitting.set(false),
    });
  }

  async acceptProposal(proposal: Proposal): Promise<void> {
    if (!this.job) return;

    const result = await Swal.fire({
      title: 'Accept proposal?',
      text: `Accept ${proposal.freelancerName || 'this freelancer'} for ${proposal.proposedBudget} ${this.job.currency}?`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Accept',
      confirmButtonColor: '#14a800',
    });

    if (!result.isConfirmed) return;

    this.jobService.acceptProposal(this.job.id, proposal.id).subscribe({
      next: () => {
        this.toast.success('Proposal accepted.');
        this.loadJob(this.job!.id);
      },
    });
  }

  async rejectProposal(proposal: Proposal): Promise<void> {
    if (!this.job) return;

    const result = await Swal.fire({
      title: 'Reject proposal?',
      text: 'Are you sure you want to reject this proposal?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Reject',
      confirmButtonColor: '#dc2626',
    });

    if (!result.isConfirmed) return;

    this.jobService.rejectProposal(this.job.id, proposal.id).subscribe({
      next: () => {
        this.toast.info('Proposal rejected.');
        this.loadProposals(this.job!.id);
      },
    });
  }

  openEditMode(): void {
    if (!this.job) return;
    this.editTitle.set(this.job.title);
    this.editDescription.set(this.job.description);
    this.editBudget.set(this.job.budget);
    this.editSkillsRaw.set(this.job.skills.join(', '));
    this.editMode.set(true);
    this.activeTab.set('details');
  }

  cancelEdit(): void {
    this.editMode.set(false);
  }

  async saveJobEdit(): Promise<void> {
    if (!this.job) return;

    const parsedSkills = this.editSkillsRaw()
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    const request: UpdateJobRequest = {
      title: this.editTitle() !== this.job.title ? this.editTitle() : undefined,
      description:
        this.editDescription() !== this.job.description
          ? this.editDescription()
          : undefined,
      budget:
        this.editBudget() !== this.job.budget ? this.editBudget() : undefined,
      skills:
        JSON.stringify(parsedSkills) !== JSON.stringify(this.job.skills)
          ? parsedSkills
          : undefined,
    };

    this.editSaving.set(true);

    this.jobService.checkEditEligibility(this.job.id, request).subscribe({
      next: async (decision: EditDecision) => {
        if (!decision.allowed) {
          this.editSaving.set(false);
          this.toast.error(decision.message);
          return;
        }

        let confirmed = false;

        if (decision.requiresConfirmation) {
          const result = await Swal.fire({
            title: 'Edit may affect proposals',
            html: `This job has <strong>${decision.activeProposalCount}</strong> active proposal(s).<br>
                   Changing <strong>${decision.sensitiveFieldsChanged.join(', ')}</strong> will mark existing
                   proposals as outdated. Freelancers will need to review and resubmit.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Apply Changes',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#14a800',
          });

          if (!result.isConfirmed) {
            this.editSaving.set(false);
            return;
          }
          confirmed = true;
        }

        this.jobService.updateJob(this.job!.id, request, confirmed).subscribe({
          next: (updated) => {
            this.job = updated;
            this.editMode.set(false);
            this.editSaving.set(false);
            const msg = confirmed
              ? 'Job updated. Affected proposals have been notified.'
              : 'Job updated successfully.';
            this.toast.success(msg);
          },
          error: () => {
            this.editSaving.set(false);
            this.toast.error('Failed to update job.');
          },
        });
      },
      error: () => {
        this.editSaving.set(false);
        this.toast.error('Failed to check edit eligibility.');
      },
    });
  }

  async confirmCancelJob(): Promise<void> {
    if (!this.job) return;

    const result = await Swal.fire({
      title: 'Cancel this job?',
      text: 'Are you sure? This will reject all active proposals.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Yes, cancel job',
      cancelButtonText: 'Keep job',
      confirmButtonColor: '#dc2626',
    });

    if (!result.isConfirmed) return;

    this.jobService.cancelJob(this.job.id).subscribe({
      next: () => {
        this.toast.info('Job cancelled successfully.');
        this.router.navigate(['/jobs']);
      },
      error: () => {
        this.toast.error('Failed to cancel job.');
      },
    });
  }

  openPaymentDialog(): void {
    if (!this.acceptedProposal) return;
    this.showPaymentModal.set(true);
  }

  closePaymentDialog(): void {
    this.showPaymentModal.set(false);
  }

  onPaymentCompleted(): void {
    this.showPaymentModal.set(false);
    if (this.job) {
      this.loadPayment(this.job.id);
    }
  }

  openReviewDialog(): void {
    this.showReviewModal.set(true);
  }

  closeReviewDialog(): void {
    this.showReviewModal.set(false);
  }

  submitReviewForm(payload: ReviewRequest): void {
    const data = this.reviewDialogData;
    if (!data.revieweeId) return;
    this.reviewSubmitting.set(true);
    this.userService.createReview(data.revieweeId, data.jobId, payload.rating, payload.comment).subscribe({
      next: () => {
        this.reviewSubmitting.set(false);
        this.showReviewModal.set(false);
        this.toast.success('Review submitted successfully.');
      },
      error: () => this.reviewSubmitting.set(false),
    });
  }

  readonly lifecycleSteps: { key: string; label: string }[] = [
    { key: 'OPEN',        label: 'Open' },
    { key: 'IN_REVIEW',   label: 'Awaiting Payment' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'COMPLETED',   label: 'Completed' },
  ];

  get lifecycleStepIndex(): number {
    return this.lifecycleSteps.findIndex((s) => s.key === this.job?.status);
  }

  get isLinearStatus(): boolean {
    return this.lifecycleStepIndex >= 0;
  }

  statusClass(status: string): string {
    if (status === 'OPEN') return 'bg-emerald-100 text-emerald-800';
    if (status === 'IN_REVIEW') return 'bg-amber-100 text-amber-800';
    if (status === 'IN_PROGRESS') return 'bg-sky-100 text-sky-800';
    if (status === 'COMPLETED' || status === 'CLOSED') return 'bg-ink-100 text-ink-600';
    if (status === 'CANCELLED' || status === 'DISPUTED') return 'bg-rose-100 text-rose-800';
    if (status === 'DRAFT') return 'bg-amber-100 text-amber-800';
    if (status === 'REFUNDED') return 'bg-violet-100 text-violet-800';
    return 'bg-ink-100 text-ink-600';
  }

  statusLabel(status: string): string {
    if (status === 'IN_REVIEW') return 'Awaiting Payment';
    return status.replace(/_/g, ' ');
  }

  proposalStatusClass(status: string): string {
    if (status === 'SUBMITTED' || status === 'PENDING') return 'bg-sky-100 text-sky-800';
    if (status === 'VIEWED') return 'bg-ink-100 text-ink-600';
    if (status === 'SHORTLISTED') return 'border border-emerald-500 bg-transparent text-emerald-700';
    if (status === 'ACCEPTED') return 'bg-emerald-600 text-white';
    if (status === 'REJECTED') return 'bg-rose-100 text-rose-800';
    if (status === 'OUTDATED') return 'bg-amber-100 text-amber-800';
    if (status === 'NEEDS_REVIEW') return 'bg-yellow-100 text-yellow-800';
    if (status === 'WITHDRAWN') return 'bg-ink-100 text-ink-400';
    return 'bg-ink-100 text-ink-600';
  }

  private loadJob(jobId: string): void {
    this.loading = true;
    this.jobService.getJob(jobId).subscribe({
      next: (job) => {
        this.job = job;
        this.loading = false;
        // Always attempt all three — backend enforces ownership via 403.
        // A successful proposals response sets ownerConfirmedViaApi and reveals the tab.
        this.loadProposals(jobId);
        this.loadMatches(jobId);
        this.loadPayment(jobId);
      },
      error: () => (this.loading = false),
    });
  }

  private loadProposals(jobId: string): void {
    this.jobService.getProposals(jobId).subscribe({
      next: (response) => {
        this.ownerConfirmedViaApi.set(true);
        this.proposals = response.content;
        // Load each freelancer's profile in parallel for richer proposal cards
        const uniqueIds = [...new Set(this.proposals.map((p) => p.freelancerId))];
        for (const id of uniqueIds) {
          this.userService.getProfile(id).subscribe({
            next: (profile) => {
              this.freelancerProfiles.update((map) => ({ ...map, [id]: profile }));
            },
          });
        }
        if (this.proposals.length > 0 && this.activeTab() === 'details') {
          this.activeTab.set('proposals');
        }
      },
      error: (err) => {
        console.error('Failed to load proposals:', err);
        /* 403 for non-owners — tab stays hidden */
      },
    });
  }

  private loadMatches(jobId: string): void {
    this.matchesLoading = true;
    this.jobService.getMatches(jobId).subscribe({
      next: (matches) => {
        this.matches = matches;
        this.matchesLoading = false;
      },
      error: () => (this.matchesLoading = false),
    });
  }

  private loadPayment(jobId: string): void {
    this.paymentService.getPaymentStatus(jobId).subscribe({
      next: (records) => (this.payment = records[0] ?? null),
      error: () => (this.payment = null),
    });
  }
}
