import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
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
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    NotificationBellComponent,
  ],
  template: `
    <mat-sidenav-container class="h-screen">
      <mat-sidenav mode="side" opened class="w-64 p-4">
        <div class="mb-6 text-xl font-bold text-blue-600">Unikly</div>
        <mat-nav-list>
          <a mat-list-item routerLink="/jobs" routerLinkActive="!bg-blue-50">
            <mat-icon matListItemIcon>work</mat-icon>
            <span matListItemTitle>Jobs</span>
          </a>
          <a mat-list-item routerLink="/search" routerLinkActive="!bg-blue-50">
            <mat-icon matListItemIcon>search</mat-icon>
            <span matListItemTitle>Search</span>
          </a>
          <a mat-list-item routerLink="/messages" routerLinkActive="!bg-blue-50">
            <mat-icon matListItemIcon>chat</mat-icon>
            <span matListItemTitle>Messages</span>
          </a>
          <a mat-list-item routerLink="/profile" routerLinkActive="!bg-blue-50">
            <mat-icon matListItemIcon>person</mat-icon>
            <span matListItemTitle>Profile</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar color="primary" class="flex justify-between">
          <span>Unikly</span>
          <div class="flex items-center gap-1">
            <app-notification-bell />
            <button mat-icon-button [matMenuTriggerFor]="userMenu">
              <mat-icon>account_circle</mat-icon>
            </button>
            <mat-menu #userMenu="matMenu">
              <button mat-menu-item routerLink="/profile">
                <mat-icon>person</mat-icon>
                <span>Profile</span>
              </button>
              <button mat-menu-item (click)="logout()">
                <mat-icon>logout</mat-icon>
                <span>Logout</span>
              </button>
            </mat-menu>
          </div>
        </mat-toolbar>

        <main class="p-0">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
})
export class MainLayoutComponent {
  constructor(private readonly keycloak: KeycloakService) {}

  logout(): void {
    this.keycloak.logout();
  }
}
