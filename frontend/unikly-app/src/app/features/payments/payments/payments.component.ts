import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { PaymentRecord, PaymentService, PaymentStatus } from '../../../core/services/payment.service';
import { ToastService } from '../../../core/services/toast.service';

const STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Awaiting Payment',
  FUNDED: 'Escrow Funded',
  RELEASED: 'Released',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  REFUNDED: 'Refunded',
  DISPUTED: 'Disputed',
};

const STATUS_CLASSES: Record<PaymentStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300',
  FUNDED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300',
  RELEASED: 'bg-sky-100 text-sky-800 dark:bg-sky-900/40 dark:text-sky-300',
  COMPLETED: 'bg-brand/10 text-brand dark:bg-brand/20',
  FAILED: 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300',
  REFUNDED: 'bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300',
  DISPUTED: 'bg-orange-100 text-orange-800 dark:bg-orange-900/40 dark:text-orange-300',
};

type FilterStatus = PaymentStatus | 'ALL';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [DatePipe, DecimalPipe, NgClass],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.scss',
})
export class PaymentsComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);
  private readonly keycloak = inject(KeycloakService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(true);
  readonly payments = signal<PaymentRecord[]>([]);
  readonly filterStatus = signal<FilterStatus>('ALL');
  readonly releasing = signal<string | null>(null);
  readonly refunding = signal<string | null>(null);

  readonly filteredPayments = computed(() => {
    const filter = this.filterStatus();
    const all = this.payments();
    return filter === 'ALL' ? all : all.filter((p) => p.status === filter);
  });

  readonly statFunded = computed(() =>
    this.payments()
      .filter((p) => p.status === 'FUNDED')
      .reduce((sum, p) => sum + p.amount, 0),
  );
  readonly statReleased = computed(() =>
    this.payments()
      .filter((p) => p.status === 'RELEASED' || p.status === 'COMPLETED')
      .reduce((sum, p) => sum + p.amount, 0),
  );
  readonly statRefunded = computed(() =>
    this.payments()
      .filter((p) => p.status === 'REFUNDED')
      .reduce((sum, p) => sum + p.amount, 0),
  );
  readonly statTotal = computed(() => this.payments().reduce((sum, p) => sum + p.amount, 0));

  readonly filters: { label: string; value: FilterStatus }[] = [
    { label: 'All', value: 'ALL' },
    { label: 'Escrow', value: 'FUNDED' },
    { label: 'Released', value: 'RELEASED' },
    { label: 'Completed', value: 'COMPLETED' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Refunded', value: 'REFUNDED' },
    { label: 'Disputed', value: 'DISPUTED' },
  ];

  isClient(): boolean {
    return this.keycloak.hasRole('ROLE_CLIENT') || this.keycloak.hasRole('CLIENT');
  }

  statusLabel(status: PaymentStatus): string {
    return STATUS_LABELS[status];
  }

  statusClasses(status: PaymentStatus): string {
    return STATUS_CLASSES[status];
  }

  ngOnInit(): void {
    this.load();
  }

  setFilter(value: FilterStatus): void {
    this.filterStatus.set(value);
  }

  release(payment: PaymentRecord): void {
    if (
      !confirm(
        `Release ${payment.amount.toFixed(2)} ${payment.currency} to the freelancer? This cannot be undone.`,
      )
    )
      return;

    this.releasing.set(payment.id);
    this.paymentService.releaseEscrow(payment.id).subscribe({
      next: () => {
        this.toast.success('Payment released to freelancer.');
        this.releasing.set(null);
        this.load();
      },
      error: () => {
        this.toast.error('Failed to release payment. Please try again.');
        this.releasing.set(null);
      },
    });
  }

  refund(payment: PaymentRecord): void {
    if (
      !confirm(
        `Request a refund of ${payment.amount.toFixed(2)} ${payment.currency}? This will initiate the refund process.`,
      )
    )
      return;

    this.refunding.set(payment.id);
    this.paymentService.requestRefund(payment.id).subscribe({
      next: () => {
        this.toast.success('Refund request submitted.');
        this.refunding.set(null);
        this.load();
      },
      error: () => {
        this.toast.error('Failed to request refund. Please try again.');
        this.refunding.set(null);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.paymentService.getAllPayments().subscribe({
      next: (records) => {
        this.payments.set(records);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
