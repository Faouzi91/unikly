import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentDialogComponent } from './payment-dialog.component';
import { PaymentService } from '../../../core/services/payment.service';
import { of } from 'rxjs';

describe('PaymentDialogComponent', () => {
  let component: PaymentDialogComponent;
  let fixture: ComponentFixture<PaymentDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentDialogComponent],
      providers: [
        {
          provide: PaymentService,
          useValue: {
            loadStripe: () => Promise.resolve(null),
            createPaymentIntent: () => of({ paymentId: 'p1', clientSecret: 'secret' }),
          },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PaymentDialogComponent);
    component = fixture.componentInstance;
    component.data = { jobId: '1', jobTitle: 'Test', budget: 100, currency: 'USD', freelancerId: 'f1' };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
