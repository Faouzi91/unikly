import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

type TokenPayload = {
  access_token: string;
  refresh_token: string;
  id_token?: string;
  expires_in: number;
  refresh_expires_in: number;
};

type DecodedToken = Record<string, unknown>;
export type SocialProvider = 'google' | 'facebook' | 'microsoft';

type SocialPendingLogin = {
  provider: SocialProvider;
  state: string;
  codeVerifier: string;
  redirectUri: string;
  requestedAt: number;
};

@Injectable({
  providedIn: 'root',
})
export class KeycloakService {
  private readonly router = inject(Router);
  private readonly storageKey = 'unikly_auth_tokens';
  private readonly socialStorageKey = 'unikly_social_login';

  private accessToken = '';
  private refreshToken = '';
  private idToken = '';
  private accessTokenExpiresAt = 0;
  private refreshTokenExpiresAt = 0;
  private tokenParsed: DecodedToken = {};

  async init(): Promise<boolean> {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return false;
    }

    try {
      const stored = JSON.parse(raw) as {
        accessToken: string;
        refreshToken: string;
        idToken: string;
        accessTokenExpiresAt: number;
        refreshTokenExpiresAt: number;
      };

      this.accessToken = stored.accessToken;
      this.refreshToken = stored.refreshToken;
      this.idToken = stored.idToken;
      this.accessTokenExpiresAt = stored.accessTokenExpiresAt;
      this.refreshTokenExpiresAt = stored.refreshTokenExpiresAt;
      this.tokenParsed = this.decodeJwt(this.accessToken);

      if (Date.now() >= this.refreshTokenExpiresAt) {
        this.clearTokens();
        return false;
      }

      if (Date.now() >= this.accessTokenExpiresAt - 30_000) {
        await this.refreshSession();
      }

      return this.isAuthenticated();
    } catch (error) {
      console.error('Token restore failed', error);
      this.clearTokens();
      return false;
    }
  }

  async login(): Promise<void> {
    await this.router.navigate(['/auth/login']);
  }

  async authenticate(username: string, password: string): Promise<void> {
    const payload = await this.postAuthRequest('/users/login', { username, password });
    this.applyTokens(payload);
  }

  async beginSocialLogin(provider: SocialProvider): Promise<void> {
    const redirectUri = `${window.location.origin}/auth/login`;
    const state = this.randomBase64Url(24);
    const codeVerifier = this.randomBase64Url(64);
    const codeChallenge = await this.sha256Base64Url(codeVerifier);

    const pending: SocialPendingLogin = {
      provider,
      state,
      codeVerifier,
      redirectUri,
      requestedAt: Date.now(),
    };
    sessionStorage.setItem(this.socialStorageKey, JSON.stringify(pending));

    const response = await fetch(`${environment.apiUrl}/users/social/authorization-url`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        provider,
        redirectUri,
        state,
        codeChallenge,
      }),
    });

    if (!response.ok) {
      const data = await response.json().catch(() => null);
      const message = data?.message || 'Unable to start social sign-in';
      throw new Error(message);
    }

    const data = (await response.json()) as { authorizationUrl?: string };
    if (!data.authorizationUrl) {
      throw new Error('Missing authorization URL');
    }

    window.location.assign(data.authorizationUrl);
  }

  async completeSocialLogin(code: string, state: string): Promise<void> {
    const pending = this.getPendingSocialLogin();
    if (!pending) {
      throw new Error('Social sign-in session expired. Please try again.');
    }

    if (pending.state !== state) {
      this.clearPendingSocialLogin();
      throw new Error('Invalid sign-in state. Please try again.');
    }

    if (Date.now() - pending.requestedAt > 10 * 60 * 1000) {
      this.clearPendingSocialLogin();
      throw new Error('Social sign-in timed out. Please try again.');
    }

    const payload = await this.postAuthRequest('/users/social/exchange', {
      code,
      redirectUri: pending.redirectUri,
      codeVerifier: pending.codeVerifier,
    });
    this.applyTokens(payload);
    this.clearPendingSocialLogin();
  }

  async logout(): Promise<void> {
    this.clearTokens();
    await this.router.navigate(['/']);
  }

  async getToken(): Promise<string> {
    if (!this.isAuthenticated()) {
      return '';
    }

    if (Date.now() >= this.accessTokenExpiresAt - 30_000) {
      try {
        await this.refreshSession();
      } catch {
        this.clearTokens();
        await this.login();
        return '';
      }
    }

    return this.accessToken;
  }

  isAuthenticated(): boolean {
    return !!this.accessToken && Date.now() < this.accessTokenExpiresAt;
  }

  getUserId(): string {
    return this.getClaimString('sub');
  }

  getRoles(): string[] {
    const realmAccess = this.tokenParsed['realm_access'] as { roles?: unknown } | undefined;
    return Array.isArray(realmAccess?.roles)
      ? realmAccess.roles.filter((role): role is string => typeof role === 'string')
      : [];
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  getUsername(): string {
    return this.getClaimString('preferred_username');
  }

  getEmail(): string {
    return this.getClaimString('email');
  }

  private async refreshSession(): Promise<void> {
    if (!this.refreshToken || Date.now() >= this.refreshTokenExpiresAt) {
      throw new Error('Refresh token expired');
    }

    const payload = await this.postAuthRequest('/users/refresh', {
      refreshToken: this.refreshToken,
    });
    this.applyTokens(payload);
  }

  private async postAuthRequest(path: string, body: Record<string, string>): Promise<TokenPayload> {
    const response = await fetch(`${environment.apiUrl}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const data = await response.json().catch(() => null);
      const message =
        data && typeof data.message === 'string' ? data.message : 'Authentication failed';
      const error = new Error(message) as Error & { status?: number };
      error.status = response.status;
      throw error;
    }

    return (await response.json()) as TokenPayload;
  }

  private getPendingSocialLogin(): SocialPendingLogin | null {
    const raw = sessionStorage.getItem(this.socialStorageKey);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as SocialPendingLogin;
    } catch {
      return null;
    }
  }

  private clearPendingSocialLogin(): void {
    sessionStorage.removeItem(this.socialStorageKey);
  }

  private applyTokens(payload: TokenPayload): void {
    if (!payload.access_token || !payload.refresh_token) {
      throw new Error('Invalid token payload');
    }

    this.accessToken = payload.access_token;
    this.refreshToken = payload.refresh_token;
    this.idToken = payload.id_token ?? '';
    this.tokenParsed = this.decodeJwt(this.accessToken);

    const now = Date.now();
    const expClaim = this.getNumericClaim('exp');
    this.accessTokenExpiresAt = payload.expires_in
      ? now + payload.expires_in * 1000
      : expClaim > 0
        ? expClaim * 1000
        : now;
    this.refreshTokenExpiresAt = payload.refresh_expires_in
      ? now + payload.refresh_expires_in * 1000
      : now + 30 * 60 * 1000;

    localStorage.setItem(
      this.storageKey,
      JSON.stringify({
        accessToken: this.accessToken,
        refreshToken: this.refreshToken,
        idToken: this.idToken,
        accessTokenExpiresAt: this.accessTokenExpiresAt,
        refreshTokenExpiresAt: this.refreshTokenExpiresAt,
      })
    );
  }

  private clearTokens(): void {
    this.accessToken = '';
    this.refreshToken = '';
    this.idToken = '';
    this.accessTokenExpiresAt = 0;
    this.refreshTokenExpiresAt = 0;
    this.tokenParsed = {};
    localStorage.removeItem(this.storageKey);
  }

  private decodeJwt(token: string): DecodedToken {
    try {
      const payload = token.split('.')[1] ?? '';
      if (!payload) {
        return {};
      }

      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
      return JSON.parse(atob(padded)) as DecodedToken;
    } catch {
      return {};
    }
  }

  private getClaimString(claim: string): string {
    const value = this.tokenParsed[claim];
    return typeof value === 'string' ? value : '';
  }

  private getNumericClaim(claim: string): number {
    const value = this.tokenParsed[claim];
    return typeof value === 'number' ? value : 0;
  }

  private randomBase64Url(bytesLength: number): string {
    const bytes = new Uint8Array(bytesLength);
    crypto.getRandomValues(bytes);
    const binary = String.fromCharCode(...bytes);
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }

  private async sha256Base64Url(value: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
    const bytes = Array.from(new Uint8Array(digest));
    const binary = String.fromCharCode(...bytes);
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }
}
