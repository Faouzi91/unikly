import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authenticatedGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.ready()) await auth.initialize();
  return auth.isAuthenticated() ? true : router.parseUrl('/login');
};

export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.ready()) await auth.initialize();
  return auth.isAuthenticated() ? router.parseUrl('/account') : true;
};


export const sellerGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.ready()) await auth.initialize();
  if (!auth.isAuthenticated()) return router.parseUrl('/login');
  return auth.user()?.role === 'SELLER' ? true : router.parseUrl('/');
};


export const buyerGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.ready()) await auth.initialize();
  if (!auth.isAuthenticated()) return router.parseUrl('/login');
  return auth.user()?.role === 'BUYER' ? true : router.parseUrl('/');
};
