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
import { PaymentService } from '../../core/services/payment.service';

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
  template: `
    <h2 mat-dialog-title class="flex items-center gap-2">
      <mat-icon class="text-green-600">lock</mat-icon>
      Fund Escrow
    </h2>

    <mat-dialog-content class="min-w-[400px]">

      <!-- Step: Summary -->
      @if (step() === 'summary') {
        <div class="space-y-4 py-2">
          <div class="bg-gray-50 rounded-lg p-4 space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">Job</span>
              <span class="font-medium">{{ data.jobTitle }}</span>
            </div>
            <mat-divider />
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">Escrow amount</span>
              <span class="font-semibold text-green-700 text-base">
                {{ data.budget | number: '1.2-2' }} {{ data.currency }}
              </span>
            </div>
          </div>
          <p class="text-xs text-gray-400">
            Funds will be held securely in escrow and only released once you confirm
            the work is complete.
          </p>
        </div>
      }

      <!-- Step: Card input (Stripe PaymentElement) -->
      @if (step() === 'card') {
        <div class="py-2 space-y-4">
          <div id="stripe-payment-element" class="min-h-[200px]">
            @if (mountingStripe()) {
              <div class="flex justify-center py-8">
                <mat-spinner diameter="32" />
              </div>
            }
          </div>
          @if (stripeError()) {
            <p class="text-red-600 text-sm flex items-center gap-1">
              <mat-icon class="!text-base">error_outline</mat-icon>
              {{ stripeError() }}
            </p>
          }
        </div>
      }

      <!-- Step: Processing -->
      @if (step() === 'processing') {
        <div class="flex flex-col items-center py-8 gap-4">
          <mat-spinner diameter="48" />
          <p class="text-gray-600">Processing your payment…</p>
        </div>
      }

    </mat-dialog-content>

    <mat-dialog-actions align="end" class="gap-2">
      <button mat-button [disabled]="step() === 'processing'" mat-dialog-close>
        Cancel
      </button>
      @if (step() === 'summary') {
        <button mat-flat-button color="primary" (click)="onContinue()">
          Continue to payment
          <mat-icon iconPositionEnd>arrow_forward</mat-icon>
        </button>
      }
      @if (step() === 'card') {
        <button
          mat-flat-button
          color="primary"
          [disabled]="mountingStripe()"
          (click)="onPayNow()"
        >
          Pay {{ data.budget | number: '1.2-2' }} {{ data.currency }}
          <mat-icon iconPositionEnd>payments</mat-icon>
        </button>
      }
    </mat-dialog-actions>
  `,
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
