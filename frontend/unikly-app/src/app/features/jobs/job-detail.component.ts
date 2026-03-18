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
  ],
  templateUrl: './job-detail.component.html',
})
export class JobDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly jobService = inject(JobService);
  private readonly keycloak = inject(KeycloakService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  job: Job | null = null;
  proposals: Proposal[] = [];
  matches: MatchEntry[] = [];
  loading = true;
  matchesLoading = false;

  get isJobOwner(): boolean {
    return this.job?.clientId === this.keycloak.getUserId();
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
