import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentDialogComponent } from './payment-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PaymentService } from '../../../core/services/payment.service';

describe('PaymentDialogComponent', () => {
  let component: PaymentDialogComponent;
  let fixture: ComponentFixture<PaymentDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: () => {} } },
        { provide: MAT_DIALOG_DATA, useValue: { jobId: '1', jobTitle: 'Test', budget: 100, currency: 'USD', freelancerId: 'f1' } },
        { provide: PaymentService, useValue: { loadStripe: () => Promise.resolve(null), createPaymentIntent: () => {} } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PaymentDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
