import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { ApiService } from './api.service';
import { environment } from '../../../environments/environment';

export type PaymentStatus =
  | 'PENDING'
  | 'FUNDED'
  | 'RELEASED'
  | 'COMPLETED'
  | 'FAILED'
  | 'REFUNDED'
  | 'DISPUTED';

export interface PaymentRecord {
  id: string;
  jobId: string;
  clientId: string;
  freelancerId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  stripePaymentIntentId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentIntentResponse {
  paymentId: string;
  clientSecret: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly api = inject(ApiService);

  private stripePromise: Promise<Stripe | null> | null = null;

  loadStripe(): Promise<Stripe | null> {
    if (!this.stripePromise) {
      this.stripePromise = loadStripe(environment.stripePublishableKey);
    }
    return this.stripePromise;
  }

  createPaymentIntent(
    jobId: string,
    freelancerId: string,
    amount: number,
    currency: string,
    idempotencyKey: string,
  ): Observable<CreatePaymentIntentResponse> {
    return this.api.post<CreatePaymentIntentResponse>('/v1/payments', {
      jobId,
      freelancerId,
      amount,
      currency,
      idempotencyKey,
    });
  }

  getAllPayments(): Observable<PaymentRecord[]> {
    return this.api.get<PaymentRecord[]>('/v1/payments/mine');
  }

  getPaymentStatus(jobId: string): Observable<PaymentRecord[]> {
    return this.api.get<PaymentRecord[]>('/v1/payments', { jobId });
  }

  verifyPayment(paymentId: string): Observable<PaymentRecord> {
    return this.api.post<PaymentRecord>(`/v1/payments/${paymentId}/verify`, {});
  }

  releaseEscrow(paymentId: string): Observable<void> {
    return this.api.post<void>(`/v1/payments/${paymentId}/release`, {});
  }

  mockFundPayment(
    jobId: string,
    freelancerId: string,
    amount: number,
    currency: string,
  ): Observable<PaymentRecord> {
    return this.api.post<PaymentRecord>('/v1/payments/dev/mock-fund', {
      jobId,
      freelancerId,
      amount,
      currency,
      idempotencyKey: crypto.randomUUID(),
    });
  }

  requestRefund(paymentId: string): Observable<void> {
    return this.api.post<void>(`/v1/payments/${paymentId}/refund`, {});
  }
}
