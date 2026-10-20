import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '@core/identity/auth.service';

type EmailRequestPurpose = 'recovery' | 'verification';

@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
})
export class ForgotPasswordPage {
  private readonly auth = inject(AuthService);
  readonly purpose = inject(ActivatedRoute).snapshot.data['purpose'] as EmailRequestPurpose ?? 'recovery';
  private readonly title = inject(Title);
  readonly busy = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  email = '';

  constructor() {
    this.title.setTitle(this.purpose === 'verification' ? 'Resend verification | Unikly' : 'Reset password | Unikly');
  }

  async submit(form: NgForm): Promise<void> {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    this.busy.set(true);
    this.error.set('');
    try {
      if (this.purpose === 'verification') {
        await this.auth.requestEmailVerification(this.email);
      } else {
        await this.auth.requestPasswordReset(this.email);
      }
      this.submitted.set(true);
    } catch {
      this.error.set('We could not process that request right now. Please wait a moment and try again.');
    } finally {
      this.busy.set(false);
    }
  }

  tryAgain(): void {
    this.submitted.set(false);
    this.error.set('');
  }
}
