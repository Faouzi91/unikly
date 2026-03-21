import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { SearchComponent } from './search.component';
import { JobService } from '../../jobs/services/job.service';
import { of } from 'rxjs';

describe('SearchComponent', () => {
  let component: SearchComponent;
  let fixture: ComponentFixture<SearchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchComponent, RouterTestingModule],
      providers: [
        { provide: JobService, useValue: { getJobs: () => of({ content: [], totalElements: 0 }), searchFreelancers: () => of({ content: [], totalElements: 0 }), getSuggestions: () => of([]) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(SearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
