import { Routes } from '@angular/router';
import { authenticatedGuard } from '@core/identity/auth.guards';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: 'account',
    canActivate: [authenticatedGuard],
    loadComponent: () =>
      import('@features/account/pages/account-page/account-page').then(
        (module) => module.AccountPage,
      ),
  },
];
