import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { KeycloakService } from '../../core/auth/keycloak.service';
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

  readonly menuOpen = signal(false);
  readonly mobileNavOpen = signal(false);

  readonly navItems = [
    { path: '/jobs', label: 'Projects' },
    { path: '/search', label: 'Talent' },
    { path: '/payments', label: 'Payments' },
    { path: '/messages', label: 'Messages' },
    { path: '/profile', label: 'Profile' },
  ];

  isAdmin(): boolean {
    return this.keycloak.hasRole('ROLE_ADMIN') || this.keycloak.hasRole('ADMIN');
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
