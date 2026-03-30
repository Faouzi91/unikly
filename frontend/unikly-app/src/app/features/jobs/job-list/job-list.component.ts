import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule, NgClass } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { ToastService } from '../../../core/services/toast.service';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { Job, JobSearchResult } from '../models/job.models';
import { JobService } from '../services/job.service';

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [CommonModule, NgClass, ReactiveFormsModule, RouterLink, TimeAgoPipe],
  templateUrl: './job-list.component.html',
  styleUrl: './job-list.component.scss',
})
export class JobListComponent implements OnInit, OnDestroy {
  private readonly jobService = inject(JobService);
  readonly keycloak = inject(KeycloakService);
  private readonly toast = inject(ToastService);
  private readonly destroy$ = new Subject<void>();
  private readonly route = inject(ActivatedRoute);

  // View mode: browse (search index) vs mine (my jobs / contracts)
  readonly viewMode = signal<'browse' | 'mine'>('browse');

  // Browse tab state (JobSearchResult shape)
  browseJobs: JobSearchResult[] = [];
  readonly loading = signal(false);
  totalElements = 0;
  currentPage = 0;
  readonly pageSize = 9;

  // "Mine" tab state (Job shape)
  myJobs: Job[] = [];
  myJobsPage = 0;
  myJobsTotalElements = 0;

  readonly searchControl = new FormControl('');
  readonly skillInputControl = new FormControl('');
  readonly minBudgetControl = new FormControl<number | null>(null);
  readonly maxBudgetControl = new FormControl<number | null>(null);

  selectedSkills: string[] = [];
  skillSuggestions: string[] = [];

  ngOnInit(): void {
    const viewParam = this.route.snapshot.queryParamMap.get('view');
    if (viewParam === 'mine' || this.isClient()) {
      this.viewMode.set('mine');
      this.loadMyJobs();
    } else {
      this.loadJobs();
    }

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadJobs();
      });

    this.minBudgetControl.valueChanges.pipe(debounceTime(300), takeUntil(this.destroy$)).subscribe(() => {
      this.currentPage = 0;
      this.loadJobs();
    });

    this.maxBudgetControl.valueChanges.pipe(debounceTime(300), takeUntil(this.destroy$)).subscribe(() => {
      this.currentPage = 0;
      this.loadJobs();
    });

    this.skillInputControl.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntil(this.destroy$))
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

  isClient(): boolean {
    return this.keycloak.hasRole('ROLE_CLIENT') || this.keycloak.hasRole('CLIENT');
  }

  isFreelancer(): boolean {
    return this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER');
  }

  mineLabel(): string {
    return this.isClient() ? 'My Projects' : 'My Contracts';
  }

  canCreateJob(): boolean {
    return this.isClient();
  }

  switchViewMode(mode: 'browse' | 'mine'): void {
    this.viewMode.set(mode);
    if (mode === 'mine' && this.myJobs.length === 0) {
      this.loadMyJobs();
    }
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements / this.pageSize));
  }

  myJobsTotalPages(): number {
    return Math.max(1, Math.ceil(this.myJobsTotalElements / this.pageSize));
  }

  addSkill(skillInput?: string): void {
    const skill = (skillInput ?? this.skillInputControl.value ?? '').trim();
    if (!skill) return;
    if (!this.selectedSkills.includes(skill)) {
      this.selectedSkills.push(skill);
      this.currentPage = 0;
      this.loadJobs();
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.selectedSkills = this.selectedSkills.filter((item) => item !== skill);
    this.currentPage = 0;
    this.loadJobs();
  }

  clearFilters(): void {
    this.searchControl.setValue('');
    this.skillInputControl.setValue('');
    this.minBudgetControl.setValue(null);
    this.maxBudgetControl.setValue(null);
    this.selectedSkills = [];
    this.currentPage = 0;
    this.loadJobs();
  }

  previousPage(): void {
    if (this.currentPage === 0) return;
    this.currentPage--;
    this.loadJobs();
  }

  nextPage(): void {
    if (this.currentPage + 1 >= this.totalPages()) return;
    this.currentPage++;
    this.loadJobs();
  }

  previousMyPage(): void {
    if (this.myJobsPage === 0) return;
    this.myJobsPage--;
    this.loadMyJobs();
  }

  nextMyPage(): void {
    if (this.myJobsPage + 1 >= this.myJobsTotalPages()) return;
    this.myJobsPage++;
    this.loadMyJobs();
  }

  getStatusClass(status: string): string {
    if (status === 'OPEN') return 'bg-emerald-100 text-emerald-800';
    if (status === 'IN_PROGRESS') return 'bg-sky-100 text-sky-800';
    if (status === 'DELIVERED') return 'bg-violet-100 text-violet-800';
    if (status === 'COMPLETED' || status === 'CLOSED') return 'bg-ink-100 text-ink-600';
    if (status === 'CANCELLED' || status === 'DISPUTED') return 'bg-rose-100 text-rose-800';
    if (status === 'DRAFT') return 'bg-amber-100 text-amber-800';
    if (status === 'REFUNDED') return 'bg-violet-100 text-violet-800';
    return 'bg-ink-100 text-ink-600';
  }

  loadMyJobs(): void {
    this.loading.set(true);
    const loader = this.isClient()
      ? this.jobService.getMyJobs(this.myJobsPage, this.pageSize)
      : this.jobService.getMyContracts(this.myJobsPage, this.pageSize);

    loader.subscribe({
      next: (response) => {
        this.myJobs = response.content;
        this.myJobsTotalElements = response.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Failed to load your projects. Please try again.');
      },
    });
  }

  private loadJobs(): void {
    this.loading.set(true);
    const params: { q?: string; skills?: string; minBudget?: number; maxBudget?: number; page: number; size: number } = {
      page: this.currentPage,
      size: this.pageSize,
    };

    const query = this.searchControl.value?.trim();
    if (query) params.q = query;
    if (this.selectedSkills.length > 0) params.skills = this.selectedSkills.join(',');
    if (this.minBudgetControl.value != null) params.minBudget = this.minBudgetControl.value;
    if (this.maxBudgetControl.value != null) params.maxBudget = this.maxBudgetControl.value;

    this.jobService.getJobs(params).subscribe({
      next: (response) => {
        this.browseJobs = response.content;
        this.totalElements = response.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Failed to load jobs. Please try again.');
      },
    });
  }
}
