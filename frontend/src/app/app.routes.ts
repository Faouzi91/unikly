import { Routes } from '@angular/router';
import { ACCOUNT_ROUTES } from '@features/account/public-api';
import { IDENTITY_ROUTES } from '@features/identity/public-api';
import { buyerGuard, sellerGuard } from '@core/identity/auth.guards';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('@features/home/public-api').then((module) => module.HomePage),
  },
  ...ACCOUNT_ROUTES,
  ...IDENTITY_ROUTES,
  {
    path: 'seller/orders',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('@features/home/pages/seller-orders-page/seller-orders-page').then(
        (module) => module.SellerOrdersPage,
      ),
  },
  {
    path: 'seller/products/new',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('@features/home/pages/seller-product-page/seller-product-page').then(
        (module) => module.SellerProductPage,
      ),
  },
  {
    path: 'seller/products',
    pathMatch: 'full',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('@features/home/pages/my-products-page/my-products-page').then(
        (module) => module.MyProductsPage,
      ),
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('@features/home/pages/product-detail-page/product-detail-page').then(
        (module) => module.ProductDetailPage,
      ),
  },
  {
    path: 'orders',
    canActivate: [buyerGuard],
    loadComponent: () =>
      import('@features/account/pages/orders-page/orders-page').then((module) => module.OrdersPage),
  },
  {
    path: 'checkout',
    loadComponent: () =>
      import('@features/home/pages/checkout-page/checkout-page').then((module) => module.CheckoutPage),
  },
  {
    path: 'basket',
    loadComponent: () =>
      import('@features/home/pages/basket-page/basket-page').then((module) => module.BasketPage),
  },
  { path: '**', redirectTo: '' },
];
