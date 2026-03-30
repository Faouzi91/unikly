import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { KeycloakService } from '../../core/auth/keycloak.service';
import { ThemeService } from '../../core/services/theme.service';
import { LoadingService } from '../../core/services/loading.service';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationBellComponent } from '../../shared/components/notification-bell/notification-bell.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

const ICONS = {
  dashboard: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
  jobs:      'M20 7H4a2 2 0 00-2 2v10a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2zm0 0V5a2 2 0 00-2-2H6a2 2 0 00-2 2v2',
  search:    'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z',
  payments:  'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z',
  messages:  'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
  admin:     'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z',
} as const;

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBellComponent, LoadingSpinnerComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent {
  private readonly keycloak = inject(KeycloakService);
  readonly theme = inject(ThemeService);
  readonly loading = inject(LoadingService);
  private readonly notificationService = inject(NotificationService);

  readonly mobileNavOpen = signal(false);
  readonly profileMenuOpen = signal(false);

  // Ensure notifications are loaded even when the bell component isn't visible (desktop)
  constructor() {
    this.notificationService.init();
  }

  readonly messageUnreadCount = computed(() =>
    this.notificationService.notifications().filter(
      (n) => !n.read && n.type === 'MESSAGE_RECEIVED',
    ).length,
  );

  get navItems(): { path: string; label: string; icon: string; queryParams?: Record<string, string> }[] {
    if (this.keycloak.hasRole('ROLE_ADMIN') || this.keycloak.hasRole('ADMIN')) {
      return [
        { path: '/admin',    label: 'Dashboard', icon: ICONS.admin },
        { path: '/jobs',     label: 'Projects',  icon: ICONS.jobs },
        { path: '/search',   label: 'People',    icon: ICONS.search },
        { path: '/payments', label: 'Payments',  icon: ICONS.payments },
      ];
    }
    if (this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER')) {
      return [
        { path: '/dashboard', label: 'Home',         icon: ICONS.dashboard },
        { path: '/jobs',      label: 'Browse Jobs',  icon: ICONS.jobs },
        { path: '/jobs',      label: 'My Contracts', icon: ICONS.jobs, queryParams: { view: 'mine' } },
        { path: '/payments',  label: 'Earnings',     icon: ICONS.payments },
        { path: '/messages',  label: 'Messages',     icon: ICONS.messages },
      ];
    }
    return [
      { path: '/dashboard', label: 'Home',        icon: ICONS.dashboard },
      { path: '/jobs',      label: 'My Projects', icon: ICONS.jobs },
      { path: '/search',    label: 'Find Talent', icon: ICONS.search },
      { path: '/payments',  label: 'Payments',    icon: ICONS.payments },
      { path: '/messages',  label: 'Messages',    icon: ICONS.messages },
    ];
  }

  getRole(): string {
    if (this.keycloak.hasRole('ROLE_ADMIN') || this.keycloak.hasRole('ADMIN')) return 'Admin';
    if (this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER')) return 'Freelancer';
    if (this.keycloak.hasRole('ROLE_CLIENT') || this.keycloak.hasRole('CLIENT')) return 'Client';
    return '';
  }

  getUsername(): string {
    return this.keycloak.getUsername() || 'User';
  }

  getInitials(): string {
    const value = this.getUsername().trim();
    if (!value) return 'UN';
    return value.slice(0, 2).toUpperCase();
  }

  closeMenu(): void {
    this.mobileNavOpen.set(false);
  }

  logout(): void {
    this.keycloak.logout();
  }
}
