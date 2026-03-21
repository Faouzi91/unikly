import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { JobDetailComponent } from './job-detail.component';
import { JobService } from '../services/job.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { PaymentService } from '../../../core/services/payment.service';
import { of } from 'rxjs';

describe('JobDetailComponent', () => {
  let component: JobDetailComponent;
  let fixture: ComponentFixture<JobDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobDetailComponent, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } },
        { provide: JobService, useValue: { getJob: () => of(null), getProposals: () => of([]), getMatches: () => of([]) } },
        { provide: KeycloakService, useValue: { hasRole: () => false, getUserId: () => 'test' } },
        { provide: PaymentService, useValue: { getPaymentStatus: () => of([]) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(JobDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
