import { Component, Input, OnInit, signal, inject, output } from '@angular/core';
import { DecimalPipe, NgClass } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import Swal from 'sweetalert2';
import { PaymentService, PaymentRecord, PaymentStatus } from '../../core/services/payment.service';
import { KeycloakService } from '../../core/auth/keycloak.service';

interface StatusDisplay {
  label: string;
  icon: string;
  classes: string;
}

const STATUS_MAP: Record<PaymentStatus, StatusDisplay> = {
  PENDING:   { label: 'Awaiting Payment',  icon: 'schedule',            classes: 'bg-amber-100 text-amber-800' },
  FUNDED:    { label: 'Escrow Funded ✓',   icon: 'lock',                classes: 'bg-green-100 text-green-800' },
  RELEASED:  { label: 'Payment Released',  icon: 'send',                classes: 'bg-blue-100 text-blue-800'  },
  COMPLETED: { label: 'Completed ✓',       icon: 'check_circle',        classes: 'bg-green-100 text-green-800' },
  FAILED:    { label: 'Payment Failed',    icon: 'error',               classes: 'bg-red-100 text-red-800'    },
  REFUNDED:  { label: 'Refunded',          icon: 'undo',                classes: 'bg-gray-100 text-gray-600'  },
  DISPUTED:  { label: 'Disputed',          icon: 'gavel',               classes: 'bg-orange-100 text-orange-800'},
};

@Component({
  selector: 'app-payment-status',
  standalone: true,
  imports: [
    DecimalPipe,
    NgClass,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  template: `
    @if (loading()) {
      <div class="flex justify-center py-4">
        <mat-spinner diameter="32" />
      </div>
    } @else if (payment()) {
      <div class="space-y-3">
        <!-- Status badge -->
        <div class="flex items-center gap-2">
          <span
            class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium"
            [ngClass]="statusDisplay().classes"
          >
            <mat-icon class="!text-base !w-4 !h-4">{{ statusDisplay().icon }}</mat-icon>
            {{ statusDisplay().label }}
          </span>
          <span class="text-gray-500 text-sm">
            {{ payment()!.amount | number: '1.2-2' }} {{ payment()!.currency }}
          </span>
        </div>

        <!-- Actions — shown only to the client when FUNDED -->
        @if (payment()!.status === 'FUNDED' && isClient()) {
          <div class="flex items-center gap-3 mt-2">
            <button
              mat-flat-button
              color="primary"
              class="bg-green-600 hover:bg-green-700"
              (click)="onRelease()"
            >
              <mat-icon>send</mat-icon>
              Release Payment
            </button>
            <button mat-button color="warn" (click)="onRefund()">
              Request Refund
            </button>
          </div>
        }
      </div>
    }
  `,
})
export class PaymentStatusComponent implements OnInit {
  @Input({ required: true }) jobId!: string;
  @Input() clientId?: string;

  readonly paymentReleased = output<void>();

  private readonly paymentService = inject(PaymentService);
  private readonly keycloak = inject(KeycloakService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly payment = signal<PaymentRecord | null>(null);

  get isClient(): () => boolean {
    return () =>
      !!this.clientId && this.clientId === this.keycloak.getUserId();
  }

  get statusDisplay(): () => StatusDisplay {
    return () => {
      const status = this.payment()?.status;
      return status ? STATUS_MAP[status] : STATUS_MAP.PENDING;
    };
  }

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.paymentService.getPaymentStatus(this.jobId).subscribe({
      next: (records) => {
        this.payment.set(records[0] ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  async onRelease(): Promise<void> {
    const p = this.payment();
    if (!p) return;

    const result = await Swal.fire({
      title: 'Release Payment?',
      text: `Release ${p.amount} ${p.currency} to the freelancer? This cannot be undone.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Release',
      confirmButtonColor: '#16a34a',
    });

    if (result.isConfirmed) {
      this.paymentService.releaseEscrow(p.id).subscribe({
        next: () => {
          this.snackBar.open('Payment released to freelancer.', 'OK', { duration: 4000 });
          this.load();
          this.paymentReleased.emit();
        },
        error: () =>
          this.snackBar.open('Failed to release payment. Please try again.', 'Close', {
            duration: 4000,
          }),
      });
    }
  }

  async onRefund(): Promise<void> {
    const p = this.payment();
    if (!p) return;

    const result = await Swal.fire({
      title: 'Request Refund?',
      text: `Refund ${p.amount} ${p.currency} back to your account?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Yes, Refund',
      confirmButtonColor: '#dc2626',
    });

    if (result.isConfirmed) {
      this.paymentService.requestRefund(p.id).subscribe({
        next: () => {
          this.snackBar.open('Refund initiated.', 'OK', { duration: 4000 });
          this.load();
        },
        error: () =>
          this.snackBar.open('Failed to request refund. Please try again.', 'Close', {
            duration: 4000,
          }),
      });
    }
  }
}
