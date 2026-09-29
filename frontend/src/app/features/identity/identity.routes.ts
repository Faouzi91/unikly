import { Routes } from '@angular/router';
import { guestGuard } from '@core/identity/auth.guards';

export const IDENTITY_ROUTES: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('@features/identity/pages/auth-page/auth-page').then((module) => module.AuthPage),
    data: { mode: 'login' },
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('@features/identity/pages/forgot-password/forgot-password').then(
        (module) => module.ForgotPasswordPage,
      ),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('@features/identity/pages/auth-page/auth-page').then((module) => module.AuthPage),
    data: { mode: 'register' },
  },
];
