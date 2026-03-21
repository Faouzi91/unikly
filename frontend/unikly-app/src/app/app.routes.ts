import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { LandingLayoutComponent } from './layouts/landing-layout/landing-layout.component';
import { LandingPageComponent } from './features/landing/landing-page/landing-page.component';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';

export const routes: Routes = [
  // Public Landing Page Routes
  {
    path: '',
    component: LandingLayoutComponent,
    children: [{ path: '', component: LandingPageComponent }],
  },

  // Integrated Auth Routes (No Sidenav)
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login.component').then(
            (m) => m.LoginComponent
          ),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register.component').then(
            (m) => m.RegisterComponent
          ),
      },
    ],
  },

  // App Routes (Authenticated)
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: 'jobs',
        loadChildren: () =>
          import('./features/jobs/jobs.routes').then((m) => m.JOBS_ROUTES),
        canActivate: [authGuard],
      },
      {
        path: 'admin',
        loadComponent: () =>
          import('./features/admin/admin-dashboard/admin-dashboard.component').then(
            (m) => m.AdminDashboardComponent
          ),
        canActivate: [authGuard, adminGuard],
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile/profile.component').then(
            (m) => m.ProfileComponent
          ),
        canActivate: [authGuard],
      },
      {
        path: 'users/:id',
        loadComponent: () =>
          import('./features/profile/public-profile/public-profile.component').then(
            (m) => m.PublicProfileComponent
          ),
      },
      {
        path: 'search',
        loadComponent: () =>
          import('./features/search/search/search.component').then(
            (m) => m.SearchComponent
          ),
        canActivate: [authGuard],
      },
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/messaging/conversation-list/conversation-list.component').then(
            (m) => m.ConversationListComponent
          ),
        canActivate: [authGuard],
      },
      {
        path: 'messages/:id',
        loadComponent: () =>
          import('./features/messaging/conversation/conversation.component').then(
            (m) => m.ConversationComponent
          ),
        canActivate: [authGuard],
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notification-list/notification-list.component').then(
            (m) => m.NotificationListComponent
          ),
        canActivate: [authGuard],
      },
    ],
  },
];
