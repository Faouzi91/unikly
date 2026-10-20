import { Routes } from '@angular/router';
import { authenticatedGuard, buyerGuard } from '@core/identity/auth.guards';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: 'orders',
    title: 'Your orders | Unikly',
    canActivate: [buyerGuard],
    loadComponent: () =>
      import('./pages/orders-page/orders-page').then((module) => module.OrdersPage),
  },
  {
    path: 'account',
    title: 'Your account | Unikly',
    canActivate: [authenticatedGuard],
    loadComponent: () =>
      import('./pages/account-page/account-page').then((module) => module.AccountPage),
  },
];
