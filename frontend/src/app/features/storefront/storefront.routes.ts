import { Routes } from '@angular/router';

export const STOREFRONT_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Unikly | Browse products',
    loadComponent: () => import('./pages/home-page/home-page').then((module) => module.HomePage),
  },
];
