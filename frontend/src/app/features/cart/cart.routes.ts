import { Routes } from '@angular/router';

export const CART_ROUTES: Routes = [
  {
    path: 'basket',
    loadComponent: () => import('./pages/basket-page/basket-page').then((module) => module.BasketPage),
  },
  {
    path: 'checkout',
    loadComponent: () =>
      import('./pages/checkout-page/checkout-page').then((module) => module.CheckoutPage),
  },
];
