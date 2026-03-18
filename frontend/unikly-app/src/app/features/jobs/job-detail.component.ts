import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';

import Swal from 'sweetalert2';

import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';
import { JobService } from './services/job.service';
import { KeycloakService } from '../../core/auth/keycloak.service';
import { Job, Proposal, MatchEntry } from './models/job.models';
import {
  ProposalDialogComponent,
  ProposalDialogData,
} from './components/proposal-dialog.component';
import {
  PaymentDialogComponent,
  PaymentDialogData,
} from '../payments/payment-dialog.component';
import { PaymentStatusComponent } from '../payments/payment-status.component';
import { PaymentService, PaymentRecord } from '../../core/services/payment.service';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTabsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatDividerModule,
    MatListModule,
    TimeAgoPipe,
    PaymentStatusComponent,
  ],
})
export class JobDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly jobService = inject(JobService);
  private readonly keycloak = inject(KeycloakService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly paymentService = inject(PaymentService);

  job: Job | null = null;
  proposals: Proposal[] = [];
  matches: MatchEntry[] = [];
  payment: PaymentRecord | null = null;
  loading = true;
  matchesLoading = false;

  get isJobOwner(): boolean {
    return this.job?.clientId === this.keycloak.getUserId();
  }

  get hasAcceptedProposal(): boolean {
    return this.proposals.some((p) => p.status === 'ACCEPTED');
  }

  get acceptedProposal(): Proposal | undefined {
    return this.proposals.find((p) => p.status === 'ACCEPTED');
  }

  get showFundEscrow(): boolean {
    return (
      this.isJobOwner &&
      this.hasAcceptedProposal &&
      this.payment === null &&
      (this.job?.status === 'IN_PROGRESS' || this.job?.status === 'OPEN')
    );
  }

  get showPaymentStatus(): boolean {
    return this.payment !== null;
  }

  get canSubmitProposal(): boolean {
    return (
      this.job?.status === 'OPEN' &&
      this.keycloak.hasRole('ROLE_FREELANCER') &&
      !this.isJobOwner
    );
  }

  ngOnInit(): void {
    const jobId = this.route.snapshot.paramMap.get('id')!;
    this.loadJob(jobId);
  }

  private loadJob(jobId: string): void {
    this.loading = true;
    this.jobService.getJob(jobId).subscribe({
      next: (job) => {
        this.job = job;
        this.loading = false;

        if (this.isJobOwner) {
          this.loadProposals(jobId);
          this.loadMatches(jobId);
          this.loadPayment(jobId);
        }
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private loadProposals(jobId: string): void {
    this.jobService.getProposals(jobId).subscribe({
      next: (proposals) => (this.proposals = proposals),
    });
  }

  private loadPayment(jobId: string): void {
    this.paymentService.getPaymentStatus(jobId).subscribe({
      next: (records) => (this.payment = records[0] ?? null),
    });
  }

  private loadMatches(jobId: string): void {
    this.matchesLoading = true;
    this.jobService.getMatches(jobId).subscribe({
      next: (matches) => {
        this.matches = matches;
        this.matchesLoading = false;
      },
      error: () => {
        this.matchesLoading = false;
      },
    });
  }

  openProposalDialog(): void {
    if (!this.job) return;

    const dialogRef = this.dialog.open(ProposalDialogComponent, {
      width: '500px',
      data: {
        jobTitle: this.job.title,
        jobBudget: this.job.budget,
        jobCurrency: this.job.currency,
      } as ProposalDialogData,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result && this.job) {
        this.jobService.submitProposal(this.job.id, result).subscribe({
          next: () => {
            this.snackBar.open('Proposal submitted!', 'Close', {
              duration: 3000,
            });
            this.loadJob(this.job!.id);
          },
        });
      }
    });
  }

  async acceptProposal(proposal: Proposal): Promise<void> {
    const result = await Swal.fire({
      title: 'Accept Proposal?',
      text: `Accept proposal from ${proposal.freelancerName || 'this freelancer'} for ${proposal.proposedBudget} ${this.job?.currency}? All other pending proposals will be rejected.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Accept',
      confirmButtonColor: '#4f46e5',
    });

    if (result.isConfirmed && this.job) {
      this.jobService
        .acceptProposal(this.job.id, proposal.id)
        .subscribe({
          next: () => {
            this.snackBar.open('Proposal accepted!', 'Close', {
              duration: 3000,
            });
            this.loadJob(this.job!.id);
          },
        });
    }
  }

  async rejectProposal(proposal: Proposal): Promise<void> {
    const result = await Swal.fire({
      title: 'Reject Proposal?',
      text: `Are you sure you want to reject this proposal?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Yes, Reject',
      confirmButtonColor: '#dc2626',
    });

    if (result.isConfirmed && this.job) {
      this.jobService
        .rejectProposal(this.job.id, proposal.id)
        .subscribe({
          next: () => {
            this.snackBar.open('Proposal rejected.', 'Close', {
              duration: 3000,
            });
            this.loadProposals(this.job!.id);
          },
        });
    }
  }

  openPaymentDialog(): void {
    if (!this.job) return;
    const accepted = this.acceptedProposal;
    if (!accepted) return;

    const dialogRef = this.dialog.open(PaymentDialogComponent, {
      width: '480px',
      disableClose: true,
      data: {
        jobId: this.job.id,
        jobTitle: this.job.title,
        budget: accepted.proposedBudget,
        currency: this.job.currency,
        freelancerId: accepted.freelancerId,
      } as PaymentDialogData,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.success && this.job) {
        this.loadPayment(this.job.id);
      }
    });
  }

  getStatusClass(status: string): Record<string, boolean> {
    return {
      'bg-green-100 text-green-800': status === 'OPEN',
      'bg-blue-100 text-blue-800': status === 'IN_PROGRESS',
      'bg-gray-100 text-gray-800': status === 'COMPLETED' || status === 'CLOSED',
      'bg-red-100 text-red-800': status === 'CANCELLED' || status === 'DISPUTED',
      'bg-yellow-100 text-yellow-800': status === 'DRAFT',
      'bg-purple-100 text-purple-800': status === 'REFUNDED',
    };
  }

  getProposalStatusClass(status: string): Record<string, boolean> {
    return {
      'bg-yellow-100 text-yellow-800': status === 'PENDING',
      'bg-green-100 text-green-800': status === 'ACCEPTED',
      'bg-red-100 text-red-800': status === 'REJECTED',
      'bg-gray-100 text-gray-800': status === 'WITHDRAWN',
    };
  }
}
