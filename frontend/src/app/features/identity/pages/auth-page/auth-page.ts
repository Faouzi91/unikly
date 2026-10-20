import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '@core/identity/auth.service';

@Component({
  selector: 'app-auth-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './auth-page.html',
  styleUrl: './auth-page.css',
})
export class AuthPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  readonly mode = inject(ActivatedRoute).snapshot.data['mode'] as 'login' | 'register';
  readonly busy = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly registrationSubmitted = signal(false);
  email = '';
  password = '';
  passwordConfirmation = '';
  displayName = '';
  accountType: 'BUYER' | 'SELLER' = 'BUYER';

  constructor() {
    this.title.setTitle(this.mode === 'login' ? 'Sign in | Unikly' : 'Create an account | Unikly');
  }

  async submit(form: NgForm): Promise<void> {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }
    if (this.mode === 'register' && this.password !== this.passwordConfirmation) {
      this.error.set('Passwords do not match.');
      return;
    }

    this.busy.set(true);
    this.error.set('');
    try {
      if (this.mode === 'register') {
        await this.auth.register({
          displayName: this.displayName,
          email: this.email,
          password: this.password,
          accountType: this.accountType,
        });
        this.registrationSubmitted.set(true);
        return;
      }
      await this.auth.login({ email: this.email, password: this.password });
      await this.router.navigateByUrl('/account');
    } catch (error: unknown) {
      this.error.set(this.messageFor(error));
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

  private messageFor(error: unknown): string {
    const status = (error as { status?: number })?.status;
    if (status === 401) return 'Email or password is incorrect. If you have not verified your email, request a new link below.';
    if (status === 429) return 'This account has reached the active session limit. Sign out from another session and try again.';
    const body = (error as { error?: { detail?: string; message?: string } })?.error;
    const message =
      body?.detail ?? body?.message ?? (body as { error?: string } | undefined)?.error;
    if (message?.toLowerCase().includes('password') || message?.includes('Bad credentials'))
      return 'Email or password is incorrect.';
    if (message) return message;
    return 'We could not complete that request. Check your details and try again.';
  }
}
