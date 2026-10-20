import { Routes } from '@angular/router';
import { ACCOUNT_ROUTES } from '@features/account/public-api';
import { CART_ROUTES } from '@features/cart/public-api';
import { CATALOG_ROUTES } from '@features/catalog/public-api';
import { IDENTITY_ROUTES } from '@features/identity/public-api';
import { SELLER_ROUTES } from '@features/seller/public-api';
import { STOREFRONT_ROUTES } from '@features/storefront/public-api';

export const routes: Routes = [
  ...ACCOUNT_ROUTES,
  ...IDENTITY_ROUTES,
  ...CATALOG_ROUTES,
  ...CART_ROUTES,
  ...SELLER_ROUTES,
  ...STOREFRONT_ROUTES,
  { path: '**', redirectTo: '' },
];
