import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { KeycloakService } from '../../core/auth/keycloak.service';
import { ThemeService } from '../../core/services/theme.service';
import { NotificationBellComponent } from '../../shared/components/notification-bell/notification-bell.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBellComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent {
  private readonly keycloak = inject(KeycloakService);
  private readonly router = inject(Router);
  readonly theme = inject(ThemeService);

  readonly menuOpen = signal(false);
  readonly mobileNavOpen = signal(false);

  get navItems(): { path: string; label: string }[] {
    if (this.keycloak.hasRole('ROLE_ADMIN') || this.keycloak.hasRole('ADMIN')) {
      return [
        { path: '/admin', label: 'Dashboard' },
        { path: '/jobs', label: 'Projects' },
        { path: '/search', label: 'People' },
        { path: '/payments', label: 'Payments' },
      ];
    }
    if (this.keycloak.hasRole('ROLE_FREELANCER') || this.keycloak.hasRole('FREELANCER')) {
      return [
        { path: '/dashboard', label: 'Home' },
        { path: '/jobs', label: 'Browse Jobs' },
        { path: '/payments', label: 'Earnings' },
        { path: '/messages', label: 'Messages' },
      ];
    }
    // Default: Client
    return [
      { path: '/dashboard', label: 'Home' },
      { path: '/jobs', label: 'My Projects' },
      { path: '/search', label: 'Find Talent' },
      { path: '/payments', label: 'Payments' },
      { path: '/messages', label: 'Messages' },
    ];
  }

  isAdmin(): boolean {
    return this.keycloak.hasRole('ROLE_ADMIN') || this.keycloak.hasRole('ADMIN');
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

  getActivePage(): string {
    const url = this.router.url;
    if (url.startsWith('/dashboard')) return 'Dashboard';
    if (url.includes('/jobs')) return 'Projects';
    if (url.includes('/search')) return 'Talent Search';
    if (url.includes('/payments')) return 'Payments';
    if (url.includes('/messages')) return 'Messages';
    if (url.includes('/profile')) return 'Profile';
    if (url.includes('/notifications')) return 'Notifications';
    if (url.includes('/admin')) return 'Admin';
    return 'Workspace';
  }

  closeMenus(): void {
    this.menuOpen.set(false);
    this.mobileNavOpen.set(false);
  }

  logout(): void {
    this.keycloak.logout();
  }
}
