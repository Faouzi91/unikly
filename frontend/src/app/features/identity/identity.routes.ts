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
    loadComponent: () =>
      import('@features/identity/pages/forgot-password/forgot-password').then(
        (module) => module.ForgotPasswordPage,
      ),
    data: { purpose: 'recovery' },
  },
  {
    path: 'resend-verification',
    loadComponent: () =>
      import('@features/identity/pages/forgot-password/forgot-password').then(
        (module) => module.ForgotPasswordPage,
      ),
    data: { purpose: 'verification' },
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('@features/identity/pages/verify-email/verify-email').then(
        (module) => module.VerifyEmailPage,
      ),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('@features/identity/pages/reset-password/reset-password').then(
        (module) => module.ResetPasswordPage,
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
