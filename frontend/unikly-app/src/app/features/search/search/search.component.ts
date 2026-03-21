import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { UserAvatarComponent } from '../../../shared/components/user-avatar/user-avatar.component';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { JobService } from '../../jobs/services/job.service';
import { FreelancerSearchResult, JobSearchResult } from '../../jobs/models/job.models';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    StarRatingComponent,
    UserAvatarComponent,
    TimeAgoPipe,
  ],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss',
})
export class SearchComponent implements OnInit, OnDestroy {
  private readonly jobService = inject(JobService);
  private readonly destroy$ = new Subject<void>();

  readonly searchControl = new FormControl('');
  readonly skillInputControl = new FormControl('');
  selectedSkills: string[] = [];
  skillSuggestions: string[] = [];
  activeTab: 'jobs' | 'freelancers' = 'jobs';
  readonly pageSize = 10;

  jobResults: JobSearchResult[] = [];
  jobsLoading = false;
  jobsPage = 0;
  jobsTotalElements = 0;

  freelancerResults: FreelancerSearchResult[] = [];
  freelancersLoading = false;
  freelancersPage = 0;
  freelancersTotalElements = 0;

  ngOnInit(): void {
    this.searchJobs();
    this.searchFreelancers();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.jobsPage = 0;
        this.freelancersPage = 0;
        this.searchJobs();
        this.searchFreelancers();
      });

    this.skillInputControl.valueChanges
      .pipe(debounceTime(220), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((value) => {
        if (value && value.trim().length >= 2) {
          this.jobService.getSuggestions(value.trim()).subscribe((suggestions) => (this.skillSuggestions = suggestions));
        } else {
          this.skillSuggestions = [];
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  jobsTotalPages(): number {
    return Math.max(1, Math.ceil(this.jobsTotalElements / this.pageSize));
  }

  freelancersTotalPages(): number {
    return Math.max(1, Math.ceil(this.freelancersTotalElements / this.pageSize));
  }

  addSkill(skillInput?: string): void {
    const raw = (skillInput ?? this.skillInputControl.value ?? '').trim();
    if (!raw) return;
    if (!this.selectedSkills.includes(raw)) {
      this.selectedSkills.push(raw);
      this.jobsPage = 0;
      this.freelancersPage = 0;
      this.searchJobs();
      this.searchFreelancers();
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.selectedSkills = this.selectedSkills.filter((item) => item !== skill);
    this.jobsPage = 0;
    this.freelancersPage = 0;
    this.searchJobs();
    this.searchFreelancers();
  }

  clearSkills(): void {
    this.selectedSkills = [];
    this.jobsPage = 0;
    this.freelancersPage = 0;
    this.searchJobs();
    this.searchFreelancers();
  }

  nextJobsPage(): void {
    if (this.jobsPage + 1 >= this.jobsTotalPages()) return;
    this.jobsPage++;
    this.searchJobs();
  }

  previousJobsPage(): void {
    if (this.jobsPage === 0) return;
    this.jobsPage--;
    this.searchJobs();
  }

  nextFreelancersPage(): void {
    if (this.freelancersPage + 1 >= this.freelancersTotalPages()) return;
    this.freelancersPage++;
    this.searchFreelancers();
  }

  previousFreelancersPage(): void {
    if (this.freelancersPage === 0) return;
    this.freelancersPage--;
    this.searchFreelancers();
  }

  private searchJobs(): void {
    this.jobsLoading = true;
    const params: { q?: string; skills?: string; page: number; size: number } = {
      page: this.jobsPage,
      size: this.pageSize,
    };

    const query = this.searchControl.value?.trim();
    if (query) params.q = query;
    if (this.selectedSkills.length > 0) params.skills = this.selectedSkills.join(',');

    this.jobService.getJobs(params).subscribe({
      next: (response) => {
        this.jobResults = response.content;
        this.jobsTotalElements = response.totalElements;
        this.jobsLoading = false;
      },
      error: () => (this.jobsLoading = false),
    });
  }

  private searchFreelancers(): void {
    this.freelancersLoading = true;
    const params: { q?: string; skills?: string; page: number; size: number } = {
      page: this.freelancersPage,
      size: this.pageSize,
    };

    const query = this.searchControl.value?.trim();
    if (query) params.q = query;
    if (this.selectedSkills.length > 0) params.skills = this.selectedSkills.join(',');

    this.jobService.searchFreelancers(params).subscribe({
      next: (response) => {
        this.freelancerResults = response.content;
        this.freelancersTotalElements = response.totalElements;
        this.freelancersLoading = false;
      },
      error: () => (this.freelancersLoading = false),
    });
  }
}
