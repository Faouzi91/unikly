import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { JobCreateComponent } from './job-create.component';
import { JobService } from '../services/job.service';
import { of } from 'rxjs';

describe('JobCreateComponent', () => {
  let component: JobCreateComponent;
  let fixture: ComponentFixture<JobCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobCreateComponent, RouterTestingModule],
      providers: [
        { provide: JobService, useValue: { getSuggestions: () => of([]), createJob: () => of({ id: '1' }) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(JobCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
