import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '@core/identity/auth.service';

type VerificationState = 'working' | 'success' | 'invalid';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  templateUrl: './verify-email.html',
})
export class VerifyEmailPage {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  readonly state = signal<VerificationState>('working');

  constructor() {
    this.title.setTitle('Verify your email | Unikly');
    void this.verify();
  }

  private async verify(): Promise<void> {
    const fragment = this.route.snapshot.fragment ?? '';
    const token = new URLSearchParams(fragment).get('token');
    if (!token) {
      this.state.set('invalid');
      return;
    }

    await this.router.navigate([], { relativeTo: this.route, fragment: '', replaceUrl: true });
    try {
      await this.auth.confirmEmailVerification(token);
      this.state.set('success');
    } catch {
      this.state.set('invalid');
    }
  }
}
