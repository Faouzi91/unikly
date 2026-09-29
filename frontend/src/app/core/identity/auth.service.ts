import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface AuthUser {
  id: number;
  email: string;
  displayName: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN';
  permissions: string[];
}

export interface CustomerProfile extends AuthUser {
  phoneNumber: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  region: string | null;
  postalCode: string | null;
  countryCode: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly user = signal<AuthUser | null>(null);
  readonly ready = signal(false);
  readonly isAuthenticated = computed(() => this.user() !== null);

  async initialize(): Promise<void> {
    await this.loadCurrentUser();
    this.ready.set(true);
  }

  async register(details: {
    displayName: string;
    email: string;
    password: string;
    accountType: 'BUYER' | 'SELLER';
  }): Promise<void> {
    await this.prepareCsrf();
    await firstValueFrom(this.http.post<AuthUser>('/api/auth/register', details));
  }

  async login(credentials: { email: string; password: string }): Promise<void> {
    await this.prepareCsrf();
    const user = await firstValueFrom(this.http.post<AuthUser>('/api/auth/login', credentials));
    this.user.set(user);
    this.ready.set(true);
  }

  async logout(): Promise<void> {
    try {
      await this.prepareCsrf();
      await firstValueFrom(this.http.post('/api/auth/logout', {}));
    } finally {
      this.user.set(null);
    }
  }

  expireSession(): void {
    this.user.set(null);
    this.ready.set(true);
  }

  async getProfile(): Promise<CustomerProfile> {
    return firstValueFrom(this.http.get<CustomerProfile>('/api/profile/me'));
  }

  async updateProfile(
    profile: Omit<CustomerProfile, 'id' | 'email' | 'role' | 'permissions'>,
  ): Promise<CustomerProfile> {
    const result = await firstValueFrom(this.http.put<CustomerProfile>('/api/profile/me', profile));
    const current = this.user();
    if (current) this.user.set({ ...current, displayName: result.displayName });
    return result;
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await firstValueFrom(this.http.get('/api/auth/csrf'));
    await firstValueFrom(this.http.post('/api/auth/password', { currentPassword, newPassword }));
    this.user.set(null);
  }

  private async loadCurrentUser(): Promise<void> {
    try {
      this.user.set(await firstValueFrom(this.http.get<AuthUser>('/api/auth/me')));
    } catch {
      this.user.set(null);
    }
  }

  private async prepareCsrf(): Promise<void> {
    await firstValueFrom(this.http.get('/api/auth/csrf'));
  }
}
