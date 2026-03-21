import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentStatusComponent } from './payment-status.component';
import { PaymentService } from '../../../core/services/payment.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { of } from 'rxjs';

describe('PaymentStatusComponent', () => {
  let component: PaymentStatusComponent;
  let fixture: ComponentFixture<PaymentStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentStatusComponent],
      providers: [
        { provide: PaymentService, useValue: { getPaymentStatus: () => of([]), releaseEscrow: () => of({}), requestRefund: () => of({}) } },
        { provide: KeycloakService, useValue: { getUserId: () => 'test' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PaymentStatusComponent);
    component = fixture.componentInstance;
    component.jobId = 'test-job';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
