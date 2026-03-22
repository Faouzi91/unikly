import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe, SlicePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { JobService } from '../../jobs/services/job.service';
import { JobSearchResult } from '../../jobs/models/job.models';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';

interface QuickAction {
  label: string;
  route: string;
  d: string;
}

const ICONS = {
  search: 'M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z',
  briefcase:
    'M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M9 5a2 2 0 0 0 2-2h2a2 2 0 0 0 2 2',
  card: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 0 0 3-3V8a3 3 0 0 0-3-3H6a3 3 0 0 0-3 3v8a3 3 0 0 0 3 3z',
  chat: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 0 1-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
  user: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z',
  dollar: 'M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6',
  users:
    'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 3a4 4 0 0 1 0 8M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75',
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, TimeAgoPipe, DecimalPipe, SlicePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly keycloak = inject(KeycloakService);
  private readonly jobService = inject(JobService);
  private readonly router = inject(Router);

  readonly jobs = signal<JobSearchResult[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);

  readonly username = computed(() => this.keycloak.getUsername() || 'there');

  readonly isClient = computed(
    () => this.keycloak.hasRole('ROLE_CLIENT') || this.keycloak.hasRole('CLIENT'),
  );
  readonly isFreelancer = computed(
    () => this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER'),
  );

  readonly clientActions: QuickAction[] = [
    { label: 'Browse Talent', route: '/search', d: ICONS.users },
    { label: 'My Projects', route: '/jobs', d: ICONS.briefcase },
    { label: 'Payments', route: '/payments', d: ICONS.card },
    { label: 'Messages', route: '/messages', d: ICONS.chat },
  ];

  readonly freelancerActions: QuickAction[] = [
    { label: 'Browse Jobs', route: '/jobs', d: ICONS.search },
    { label: 'My Profile', route: '/profile', d: ICONS.user },
    { label: 'Earnings', route: '/payments', d: ICONS.dollar },
    { label: 'Messages', route: '/messages', d: ICONS.chat },
  ];

  get quickActions(): QuickAction[] {
    return this.isClient() ? this.clientActions : this.freelancerActions;
  }

  ngOnInit(): void {
    if (this.keycloak.hasRole('ROLE_ADMIN') || this.keycloak.hasRole('ADMIN')) {
      this.router.navigate(['/admin']);
      return;
    }
    this.jobService.getJobs({ page: 0, size: 6 }).subscribe({
      next: (res) => {
        this.jobs.set(res.content);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }
}
