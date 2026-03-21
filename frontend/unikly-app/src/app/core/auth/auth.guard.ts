import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from './keycloak.service';

export const authGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  if (keycloak.isAuthenticated()) {
    return true;
  }

  // Redirect to internal login page
  return router.parseUrl('/auth/login');
};
