import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService, CustomerProfile } from '@core/identity/auth.service';

@Component({
  selector: 'app-account-page',
  imports: [FormsModule],
  templateUrl: './account-page.html',
  styleUrl: './account-page.css',
})
export class AccountPage {
  readonly auth = inject(AuthService);
  readonly profile = signal<CustomerProfile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly passwordSaving = signal(false);
  readonly status = signal('');
  readonly error = signal('');
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  constructor() {
    void this.loadProfile();
  }

  async loadProfile(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.profile.set(await this.auth.getProfile());
    } catch {
      this.error.set('We could not load your profile. Please refresh and try again.');
    } finally {
      this.loading.set(false);
    }
  }

  async saveProfile(): Promise<void> {
    const profile = this.profile();
    if (!profile || this.saving()) return;
    this.saving.set(true);
    this.status.set('');
    this.error.set('');
    try {
      this.profile.set(
        await this.auth.updateProfile({
          displayName: profile.displayName,
          phoneNumber: profile.phoneNumber,
          addressLine1: profile.addressLine1,
          addressLine2: profile.addressLine2,
          city: profile.city,
          region: profile.region,
          postalCode: profile.postalCode,
          countryCode: profile.countryCode,
        }),
      );
      this.status.set('Your profile has been updated.');
    } catch {
      this.error.set('We could not save your profile. Check the details and try again.');
    } finally {
      this.saving.set(false);
    }
  }

  async savePassword(): Promise<void> {
    this.status.set('');
    this.error.set('');
    if (this.newPassword !== this.confirmPassword) {
      this.error.set('The new passwords do not match.');
      return;
    }
    this.passwordSaving.set(true);
    try {
      await this.auth.changePassword(this.currentPassword, this.newPassword);
      window.location.assign('/login?passwordChanged=1');
    } catch {
      this.error.set(
        'We could not change your password. Check your current password and try again.',
      );
    } finally {
      this.passwordSaving.set(false);
    }
  }

  updateField(field: keyof CustomerProfile, value: string): void {
    const profile = this.profile();
    if (profile) this.profile.set({ ...profile, [field]: value || null });
  }

  get permissionSummary(): string {
    const permissions = this.auth.user()?.permissions ?? [];
    return permissions.length
      ? permissions.map((permission) => this.permissionLabel(permission)).join(', ')
      : 'None';
  }

  private permissionLabel(permission: string): string {
    const labels: Record<string, string> = {
      ACCOUNT_READ_SELF: 'View own account',
      ACCOUNT_UPDATE_SELF: 'Update own account',
      CATALOG_READ: 'Browse catalog',
      CART_MANAGE_SELF: 'Manage own cart',
      ORDER_CREATE_SELF: 'Place orders',
      ORDER_READ_SELF: 'View own orders',
      ORDER_CANCEL_SELF: 'Cancel own orders',
      PLATFORM_ADMIN: 'Administer platform',
      ACCOUNT_READ_ANY: 'View customer accounts',
      ACCOUNT_SUSPEND: 'Suspend accounts',
      CATALOG_MANAGE: 'Manage catalog',
      ORDER_MANAGE: 'Manage orders',
      ORDER_REFUND: 'Issue refunds',
      REVIEW_MODERATE: 'Moderate reviews',
      ROLE_ASSIGN: 'Assign roles',
      PLATFORM_REPORT_READ: 'View platform reports',
    };
    return labels[permission] ?? permission;
  }
}
