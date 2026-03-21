import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatBadgeModule } from '@angular/material/badge';
import { MatExpansionModule } from '@angular/material/expansion';

import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { JobService } from '../services/job.service';
import { JobSearchResult } from '../models/job.models';
import { KeycloakService } from '../../../core/auth/keycloak.service';

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatAutocompleteModule,
    MatBadgeModule,
    MatExpansionModule,
    TimeAgoPipe,
  ],
  templateUrl: './job-list.component.html',
  styleUrl: './job-list.component.scss',
})
export class JobListComponent implements OnInit, OnDestroy {
  private readonly jobService = inject(JobService);
  readonly keycloak = inject(KeycloakService);
  private readonly destroy$ = new Subject<void>();

  jobs: JobSearchResult[] = [];
  loading = false;
  totalElements = 0;
  currentPage = 0;
  pageSize = 9;

  searchControl = new FormControl('');
  skillInputControl = new FormControl('');
  minBudgetControl = new FormControl<number | null>(null);
  maxBudgetControl = new FormControl<number | null>(null);

  selectedSkills: string[] = [];
  skillSuggestions: string[] = [];

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  ngOnInit(): void {
    this.loadJobs();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadJobs();
      });

    this.minBudgetControl.valueChanges
      .pipe(debounceTime(300), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadJobs();
      });

    this.maxBudgetControl.valueChanges
      .pipe(debounceTime(300), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadJobs();
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

  loadJobs(): void {
    this.loading = true;
    const params: {
      q?: string;
      skills?: string;
      minBudget?: number;
      maxBudget?: number;
      page: number;
      size: number;
    } = {
      page: this.currentPage,
      size: this.pageSize,
    };

    const q = this.searchControl.value?.trim();
    if (q) params.q = q;
    if (this.selectedSkills.length > 0)
      params.skills = this.selectedSkills.join(',');
    if (this.minBudgetControl.value != null)
      params.minBudget = this.minBudgetControl.value;
    if (this.maxBudgetControl.value != null)
      params.maxBudget = this.maxBudgetControl.value;

    this.jobService.getJobs(params).subscribe({
      next: (response) => {
        this.jobs = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  addSkill(event: MatAutocompleteSelectedEvent): void {
    const skill = event.option.value;
    if (!this.selectedSkills.includes(skill)) {
      this.selectedSkills.push(skill);
      this.currentPage = 0;
      this.loadJobs();
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.selectedSkills = this.selectedSkills.filter((s) => s !== skill);
    this.currentPage = 0;
    this.loadJobs();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadJobs();
  }

  getStatusClass(status: string): Record<string, boolean> {
    return {
      'bg-green-100 text-green-800': status === 'OPEN',
      'bg-blue-100 text-blue-800': status === 'IN_PROGRESS',
      'bg-gray-100 text-gray-800': status === 'COMPLETED' || status === 'CLOSED',
      'bg-red-100 text-red-800':
        status === 'CANCELLED' || status === 'DISPUTED',
      'bg-yellow-100 text-yellow-800': status === 'DRAFT',
      'bg-purple-100 text-purple-800': status === 'REFUNDED',
    };
  }
}
