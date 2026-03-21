import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class KeycloakService {
  private keycloak: Keycloak;
  private router = inject(Router);

  constructor() {
    this.keycloak = new Keycloak({
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId,
    });
  }

  async init(): Promise<boolean> {
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        silentCheckSsoRedirectUri:
          window.location.origin + '/assets/silent-check-sso.html',
      });
      return authenticated;
    } catch (error) {
      console.error('Keycloak init failed', error);
      return false;
    }
  }

  async login(): Promise<void> {
    await this.router.navigate(['/auth/login']);
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }

  async getToken(): Promise<string> {
    if (!this.keycloak.authenticated) {
      return '';
    }
    try {
      await this.keycloak.updateToken(30);
    } catch {
      await this.login();
    }
    return this.keycloak.token ?? '';
  }

  isAuthenticated(): boolean {
    return !!this.keycloak.authenticated;
  }

  async loginWithCredentials(username: string, password: string): Promise<boolean> {
    try {
      // Manual login using Direct Access Grant
      await this.keycloak.login({
        scope: 'openid',
        // Note: keycloak-js doesn't natively support password grant directly in the login() call 
        // without redirection. We'll use a fetch to the token endpoint instead.
      });
      return true;
    } catch (error) {
      console.error('Login failed', error);
      return false;
    }
  }

  // Refined login method for custom UI
  async getTokenWithPassword(username: string, password: string): Promise<void> {
    const details: Record<string, string> = {
      'client_id': environment.keycloak.clientId,
      'grant_type': 'password',
      'username': username,
      'password': password,
      'scope': 'openid profile email'
    };

    const formBody = Object.keys(details).map(key => encodeURIComponent(key) + '=' + encodeURIComponent(details[key])).join('&');

    const response = await fetch(`${environment.keycloak.url}/realms/${environment.keycloak.realm}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
      },
      body: formBody
    });

    if (!response.ok) {
      throw new Error('Authentication failed');
    }

    const tokens = await response.json();
    // Manually set tokens in keycloak-js instance
    await this.keycloak.init({
      onLoad: 'check-sso',
      token: tokens.access_token,
      refreshToken: tokens.refresh_token,
      idToken: tokens.id_token
    });
  }

  getUserId(): string {
    return this.keycloak.subject ?? '';
  }

  getRoles(): string[] {
    return this.keycloak.realmAccess?.roles ?? [];
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  getUsername(): string {
    return this.keycloak.tokenParsed?.['preferred_username'] ?? '';
  }

  getEmail(): string {
    return this.keycloak.tokenParsed?.['email'] ?? '';
  }
}
