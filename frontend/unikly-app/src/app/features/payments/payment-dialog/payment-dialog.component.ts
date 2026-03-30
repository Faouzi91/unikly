import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Stripe, StripeElements } from '@stripe/stripe-js';
import { PaymentService } from '../../../core/services/payment.service';
import { ToastService } from '../../../core/services/toast.service';

export interface PaymentDialogData {
  jobId: string;
  jobTitle: string;
  budget: number;
  currency: string;
  freelancerId: string;
}

type DialogStep = 'summary' | 'card' | 'processing';

@Component({
  selector: 'app-payment-dialog',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './payment-dialog.component.html',
  styleUrl: './payment-dialog.component.scss',
})
export class PaymentDialogComponent implements OnInit, OnDestroy {
  private readonly paymentService = inject(PaymentService);
  private readonly toast = inject(ToastService);

  @Input({ required: true }) data!: PaymentDialogData;
  @Output() close = new EventEmitter<void>();
  @Output() paid = new EventEmitter<{ paymentId: string | null }>();

  readonly step = signal<DialogStep>('summary');
  readonly mountingStripe = signal(false);
  readonly stripeError = signal<string | null>(null);

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentId: string | null = null;

  ngOnInit(): void {
    this.paymentService.loadStripe().then((instance) => (this.stripe = instance));
  }

  async onContinue(): Promise<void> {
    this.step.set('card');
    this.mountingStripe.set(true);
    this.stripeError.set(null);

    try {
      const idempotencyKey = crypto.randomUUID();
      const payload = await new Promise<{ paymentId: string; clientSecret: string }>((resolve, reject) => {
        this.paymentService
          .createPaymentIntent(this.data.jobId, this.data.freelancerId, this.data.budget, this.data.currency, idempotencyKey)
          .subscribe({ next: resolve, error: reject });
      });

      this.paymentId = payload.paymentId;

      if (!this.stripe) {
        this.stripe = await this.paymentService.loadStripe();
      }
      if (!this.stripe) {
        throw new Error('Stripe could not be loaded.');
      }

      this.elements?.getElement('payment')?.destroy();
      this.elements = this.stripe.elements({ clientSecret: payload.clientSecret });
      const paymentElement = this.elements.create('payment');
      paymentElement.mount('#stripe-payment-element');
      this.mountingStripe.set(false);
    } catch (err) {
      let message = 'Failed to initialize payment.';
      if (err instanceof HttpErrorResponse) {
        message = err.error?.message ?? `Request failed (${err.status}). Please try again.`;
      } else if (err instanceof Error) {
        message = err.message;
      }
      this.stripeError.set(message);
      this.mountingStripe.set(false);
    }
  }

  async onPayNow(): Promise<void> {
    if (!this.stripe || !this.elements) return;

    this.step.set('processing');
    this.stripeError.set(null);

    const { error } = await this.stripe.confirmPayment({
      elements: this.elements,
      redirect: 'if_required',
      confirmParams: {
        return_url: window.location.origin + '/jobs/' + this.data.jobId,
      },
    });

    if (error) {
      this.step.set('card');
      this.stripeError.set(error.message ?? 'Payment failed. Please try again.');
      return;
    }

    // Verify payment status directly with Stripe (don't rely on webhooks)
    if (this.paymentId) {
      this.paymentService.verifyPayment(this.paymentId).subscribe({
        next: (payment) => {
          if (payment.status === 'FUNDED') {
            this.toast.success('Escrow funded successfully.');
          } else {
            this.toast.success('Payment submitted. Confirming with Stripe...');
          }
          this.paid.emit({ paymentId: this.paymentId });
        },
        error: () => {
          this.toast.success('Payment submitted. It may take a moment to confirm.');
          this.paid.emit({ paymentId: this.paymentId });
        },
      });
    } else {
      this.paid.emit({ paymentId: this.paymentId });
    }
  }

  onClose(): void {
    if (this.step() === 'processing') return;
    this.close.emit();
  }

  ngOnDestroy(): void {
    this.elements?.getElement('payment')?.destroy();
  }
}
