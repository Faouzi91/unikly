import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'jobs', pathMatch: 'full' },
      {
        path: 'jobs',
        loadChildren: () =>
          import('./features/jobs/jobs.routes').then((m) => m.JOBS_ROUTES),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then(
            (m) => m.ProfileComponent
          ),
        canActivate: [authGuard],
      },
      {
        path: 'users/:id',
        loadComponent: () =>
          import('./features/profile/public-profile.component').then(
            (m) => m.PublicProfileComponent
          ),
      },
      {
        path: 'search',
        loadComponent: () =>
          import('./features/search/search.component').then(
            (m) => m.SearchComponent
          ),
      },
      {
        path: 'messages',
        loadComponent: () =>
          import('./features/messaging/conversation-list.component').then(
            (m) => m.ConversationListComponent
          ),
        canActivate: [authGuard],
      },
      {
        path: 'messages/:id',
        loadComponent: () =>
          import('./features/messaging/conversation.component').then(
            (m) => m.ConversationComponent
          ),
        canActivate: [authGuard],
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import(
            './features/notifications/notification-list.component'
          ).then((m) => m.NotificationListComponent),
        canActivate: [authGuard],
      },
    ],
  },
];
