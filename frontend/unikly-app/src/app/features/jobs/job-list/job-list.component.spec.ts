import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { JobListComponent } from './job-list.component';
import { JobService } from '../services/job.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { of } from 'rxjs';

describe('JobListComponent', () => {
  let component: JobListComponent;
  let fixture: ComponentFixture<JobListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobListComponent, RouterTestingModule],
      providers: [
        { provide: JobService, useValue: { getJobs: () => of({ content: [], totalElements: 0 }), getSuggestions: () => of([]) } },
        { provide: KeycloakService, useValue: { hasRole: () => false, getUserId: () => 'test' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(JobListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
