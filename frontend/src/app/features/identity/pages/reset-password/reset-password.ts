import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '@core/identity/auth.service';

type ResetState = 'form' | 'success' | 'invalid';

@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private token = '';
  readonly state = signal<ResetState>('form');
  readonly busy = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  newPassword = '';
  confirmPassword = '';

  constructor() {
    this.title.setTitle('Choose a new password | Unikly');
    const fragment = this.route.snapshot.fragment ?? '';
    this.token = new URLSearchParams(fragment).get('token') ?? '';
    if (!this.token) {
      this.state.set('invalid');
    } else {
      void this.router.navigate([], { relativeTo: this.route, fragment: '', replaceUrl: true });
    }
  }

  async submit(form: NgForm): Promise<void> {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.error.set('Passwords do not match.');
      return;
    }

    this.busy.set(true);
    this.error.set('');
    try {
      await this.auth.resetPassword(this.token, this.newPassword);
      this.state.set('success');
    } catch {
      this.state.set('invalid');
    } finally {
      this.busy.set(false);
    }
  }

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  clearError(): void {
    this.error.set('');
  }
}
