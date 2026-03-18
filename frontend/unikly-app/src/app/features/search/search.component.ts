import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { StarRatingComponent } from '../../shared/components/star-rating/star-rating.component';
import { UserAvatarComponent } from '../../shared/components/user-avatar/user-avatar.component';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';
import { JobService } from '../jobs/services/job.service';
import {
  JobSearchResult,
  FreelancerSearchResult,
} from '../jobs/models/job.models';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatPaginatorModule,
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

  searchControl = new FormControl('');
  skillInputControl = new FormControl('');
  selectedSkills: string[] = [];
  skillSuggestions: string[] = [];
  activeTab = 0;
  pageSize = 10;

  // Jobs
  jobResults: JobSearchResult[] = [];
  jobsLoading = false;
  jobsPage = 0;
  jobsTotalElements = 0;

  // Freelancers
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
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((value) => {
        if (value && value.length >= 2) {
          this.jobService
            .getSuggestions(value)
            .subscribe((suggestions) => (this.skillSuggestions = suggestions));
        } else {
          this.skillSuggestions = [];
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private searchJobs(): void {
    this.jobsLoading = true;
    const params: {
      q?: string;
      skills?: string;
      page: number;
      size: number;
    } = {
      page: this.jobsPage,
      size: this.pageSize,
    };

    const q = this.searchControl.value?.trim();
    if (q) params.q = q;
    if (this.selectedSkills.length > 0)
      params.skills = this.selectedSkills.join(',');

    this.jobService.getJobs(params).subscribe({
      next: (response) => {
        this.jobResults = response.content;
        this.jobsTotalElements = response.totalElements;
        this.jobsLoading = false;
      },
      error: () => {
        this.jobsLoading = false;
      },
    });
  }

  private searchFreelancers(): void {
    this.freelancersLoading = true;
    const params: {
      q?: string;
      skills?: string;
      page: number;
      size: number;
    } = {
      page: this.freelancersPage,
      size: this.pageSize,
    };

    const q = this.searchControl.value?.trim();
    if (q) params.q = q;
    if (this.selectedSkills.length > 0)
      params.skills = this.selectedSkills.join(',');

    this.jobService.searchFreelancers(params).subscribe({
      next: (response) => {
        this.freelancerResults = response.content;
        this.freelancersTotalElements = response.totalElements;
        this.freelancersLoading = false;
      },
      error: () => {
        this.freelancersLoading = false;
      },
    });
  }

  addSkill(event: MatAutocompleteSelectedEvent): void {
    const skill = event.option.value;
    if (!this.selectedSkills.includes(skill)) {
      this.selectedSkills.push(skill);
      this.jobsPage = 0;
      this.freelancersPage = 0;
      this.searchJobs();
      this.searchFreelancers();
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.selectedSkills = this.selectedSkills.filter((s) => s !== skill);
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

  onTabChange(index: number): void {
    this.activeTab = index;
  }

  onJobsPageChange(event: PageEvent): void {
    this.jobsPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.searchJobs();
  }

  onFreelancersPageChange(event: PageEvent): void {
    this.freelancersPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.searchFreelancers();
  }
}
