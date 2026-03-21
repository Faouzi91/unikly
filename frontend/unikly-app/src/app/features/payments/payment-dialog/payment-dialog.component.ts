import {
  Component,
  Inject,
  OnDestroy,
  OnInit,
  signal,
  inject,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { Stripe, StripeElements } from '@stripe/stripe-js';
import { PaymentService } from '../../../core/services/payment.service';

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
  imports: [
    DecimalPipe,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
  ],
  templateUrl: './payment-dialog.component.html',
  styleUrl: './payment-dialog.component.scss',
})
export class PaymentDialogComponent implements OnInit, OnDestroy {
  private readonly paymentService = inject(PaymentService);
  private readonly snackBar = inject(MatSnackBar);

  readonly step = signal<DialogStep>('summary');
  readonly mountingStripe = signal(false);
  readonly stripeError = signal<string | null>(null);

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentId: string | null = null;

  constructor(
    public readonly dialogRef: MatDialogRef<PaymentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PaymentDialogData,
  ) {}

  ngOnInit(): void {
    // Pre-load Stripe.js in the background while the user reads the summary
    this.paymentService.loadStripe().then((s) => (this.stripe = s));
  }

  async onContinue(): Promise<void> {
    this.step.set('card');
    this.mountingStripe.set(true);

    try {
      const idempotencyKey = crypto.randomUUID();
      const result = await new Promise<{ paymentId: string; clientSecret: string }>(
        (resolve, reject) => {
          this.paymentService
            .createPaymentIntent(
              this.data.jobId,
              this.data.freelancerId,
              this.data.budget,
              this.data.currency,
              idempotencyKey,
            )
            .subscribe({ next: resolve, error: reject });
        },
      );

      this.paymentId = result.paymentId;

      if (!this.stripe) {
        this.stripe = await this.paymentService.loadStripe();
      }
      if (!this.stripe) {
        throw new Error('Stripe.js failed to load');
      }

      this.elements = this.stripe.elements({ clientSecret: result.clientSecret });
      const paymentElement = this.elements.create('payment');
      paymentElement.mount('#stripe-payment-element');
      this.mountingStripe.set(false);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to initialize payment';
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
    } else {
      this.snackBar.open('Payment processing — escrow will be funded shortly.', 'OK', {
        duration: 5000,
      });
      this.dialogRef.close({ success: true, paymentId: this.paymentId });
    }
  }

  ngOnDestroy(): void {
    this.elements?.getElement('payment')?.destroy();
  }
}
