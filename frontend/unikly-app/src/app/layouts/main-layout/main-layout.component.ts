import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { KeycloakService } from '../../core/auth/keycloak.service';
import { NotificationBellComponent } from '../../shared/components/notification-bell/notification-bell.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    NotificationBellComponent,
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent {
  constructor(
    private readonly keycloak: KeycloakService,
    private readonly router: Router
  ) {}

  isAdmin(): boolean {
    return this.keycloak.hasRole('ADMIN');
  }

  getUsername(): string {
    return this.keycloak.getUsername();
  }

  getInitials(): string {
    const user = this.getUsername();
    return user ? user.substring(0, 2).toUpperCase() : 'UN';
  }

  getActivePage(): string {
    const url = this.router.url;
    if (url.includes('jobs')) return 'Projects';
    if (url.includes('search')) return 'Talent Search';
    if (url.includes('messages')) return 'Collaboration';
    if (url.includes('profile')) return 'Account';
    if (url.includes('admin')) return 'System Control';
    return 'Platform';
  }

  logout(): void {
    this.keycloak.logout();
  }
}
