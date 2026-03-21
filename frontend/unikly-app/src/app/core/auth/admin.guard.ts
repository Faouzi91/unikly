import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakService } from './keycloak.service';

export const adminGuard = () => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  if (keycloakService.isAuthenticated() && keycloakService.hasRole('ADMIN')) {
    return true;
  }

  // Redirect to home if not admin
  return router.parseUrl('/');
};
