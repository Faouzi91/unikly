import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from './keycloak.service';

export function roleGuard(requiredRole: string): CanActivateFn {
  return () => {
    const keycloak = inject(KeycloakService);
    const router = inject(Router);

    if (keycloak.hasRole(requiredRole)) {
      return true;
    }

    return router.createUrlTree(['/']);
  };
}
