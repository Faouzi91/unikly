import { Routes } from '@angular/router';

export const CATALOG_ROUTES: Routes = [
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./pages/product-detail-page/product-detail-page').then(
        (module) => module.ProductDetailPage,
      ),
  },
];
