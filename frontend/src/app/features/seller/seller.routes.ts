import { Routes } from '@angular/router';
import { sellerGuard } from '@core/identity/auth.guards';

export const SELLER_ROUTES: Routes = [
  {
    path: 'seller/orders',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('./pages/seller-orders-page/seller-orders-page').then(
        (module) => module.SellerOrdersPage,
      ),
  },
  {
    path: 'seller/products/new',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('./pages/seller-product-page/seller-product-page').then(
        (module) => module.SellerProductPage,
      ),
  },
  {
    path: 'seller/products',
    pathMatch: 'full',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('./pages/my-products-page/my-products-page').then((module) => module.MyProductsPage),
  },
];
