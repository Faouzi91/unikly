import { Component, Input, OnInit, inject, output, signal } from '@angular/core';
import { DecimalPipe, NgClass } from '@angular/common';
import Swal from 'sweetalert2';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { PaymentRecord, PaymentService, PaymentStatus } from '../../../core/services/payment.service';
import { ToastService } from '../../../core/services/toast.service';

interface StatusDisplay {
  label: string;
  classes: string;
}

const STATUS_MAP: Record<PaymentStatus, StatusDisplay> = {
  PENDING: { label: 'Awaiting Payment', classes: 'bg-amber-100 text-amber-800' },
  FUNDED: { label: 'Escrow Funded', classes: 'bg-emerald-100 text-emerald-800' },
  RELEASED: { label: 'Payment Released', classes: 'bg-sky-100 text-sky-800' },
  COMPLETED: { label: 'Completed', classes: 'bg-emerald-100 text-emerald-800' },
  FAILED: { label: 'Payment Failed', classes: 'bg-rose-100 text-rose-800' },
  REFUNDED: { label: 'Refunded', classes: 'bg-ink-100 text-ink-600' },
  DISPUTED: { label: 'Disputed', classes: 'bg-orange-100 text-orange-800' },
};

@Component({
  selector: 'app-payment-status',
  standalone: true,
  imports: [DecimalPipe, NgClass],
  templateUrl: './payment-status.component.html',
  styleUrl: './payment-status.component.scss',
})
export class PaymentStatusComponent implements OnInit {
  @Input({ required: true }) jobId!: string;
  @Input() clientId?: string;

  readonly paymentReleased = output<void>();

  private readonly paymentService = inject(PaymentService);
  private readonly keycloak = inject(KeycloakService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly payment = signal<PaymentRecord | null>(null);

  ngOnInit(): void {
    this.load();
  }

  isClient(): boolean {
    return !!this.clientId && this.clientId === this.keycloak.getUserId();
  }

  statusDisplay(): StatusDisplay {
    const status = this.payment()?.status;
    return status ? STATUS_MAP[status] : STATUS_MAP.PENDING;
  }

  async onRelease(): Promise<void> {
    const payment = this.payment();
    if (!payment) return;

    const result = await Swal.fire({
      title: 'Release payment?',
      text: `Release ${payment.amount} ${payment.currency} to freelancer?`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Release',
      confirmButtonColor: '#14a800',
    });

    if (!result.isConfirmed) return;

    this.paymentService.releaseEscrow(payment.id).subscribe({
      next: () => {
        this.toast.success('Payment released to freelancer.');
        this.load();
        this.paymentReleased.emit();
      },
      error: () => this.toast.error('Failed to release payment.'),
    });
  }

  async onRefund(): Promise<void> {
    const payment = this.payment();
    if (!payment) return;

    const result = await Swal.fire({
      title: 'Request refund?',
      text: `Refund ${payment.amount} ${payment.currency} back to your account?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Request refund',
      confirmButtonColor: '#dc2626',
    });

    if (!result.isConfirmed) return;

    this.paymentService.requestRefund(payment.id).subscribe({
      next: () => {
        this.toast.success('Refund request submitted.');
        this.load();
      },
      error: () => this.toast.error('Failed to request refund.'),
    });
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
}
