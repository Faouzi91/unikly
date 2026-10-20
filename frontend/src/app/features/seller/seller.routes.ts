import { Routes } from '@angular/router';
import { sellerGuard } from '@core/identity/auth.guards';

export const SELLER_ROUTES: Routes = [
  {
    path: 'seller/orders',
    title: 'Orders to fulfill | Unikly',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('./pages/seller-orders-page/seller-orders-page').then(
        (module) => module.SellerOrdersPage,
      ),
  },
  {
    path: 'seller/products/new',
    title: 'Sell a product | Unikly',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('./pages/seller-product-page/seller-product-page').then(
        (module) => module.SellerProductPage,
      ),
  },
  {
    path: 'seller/products',
    pathMatch: 'full',
    title: 'My products | Unikly',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('./pages/my-products-page/my-products-page').then((module) => module.MyProductsPage),
  },
];
