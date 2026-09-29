import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  readonly mode = inject(ActivatedRoute).snapshot.data['mode'] as 'login' | 'register';
  readonly busy = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  email = '';
  password = '';
  displayName = '';
  accountType: 'BUYER' | 'SELLER' = 'BUYER';

  async submit(): Promise<void> {
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

  private messageFor(error: unknown): string {
    const body = (error as { error?: { detail?: string; message?: string } })?.error;
    const message =
      body?.detail ?? body?.message ?? (body as { error?: string } | undefined)?.error;
    if (message?.includes('already exists'))
      return 'An account already uses this email. Try signing in.';
    if (message?.toLowerCase().includes('password') || message?.includes('Bad credentials'))
      return 'Email or password is incorrect.';
    if (message) return message;
    return 'We could not complete that request. Check your details and try again.';
  }
}
