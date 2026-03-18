import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { KeycloakService } from './keycloak.service';

export const authGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);

  if (keycloak.isAuthenticated()) {
    return true;
  }

  await keycloak.login();
  return false;
};
