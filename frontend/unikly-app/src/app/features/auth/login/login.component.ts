import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { KeycloakService, SocialProvider } from '../../../core/auth/keycloak.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  readonly theme = inject(ThemeService);
  private readonly fb = inject(FormBuilder);
  private readonly keycloak = inject(KeycloakService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly loadingSocial = signal<SocialProvider | null>(null);
  readonly error = signal<string | null>(null);
  submitted = false;

  readonly metrics = [
    { value: '12k+', label: 'Vetted experts' },
    { value: '98%', label: 'Match precision' },
    { value: '4.9/5', label: 'Client rating' },
  ];

  readonly socialProviders: Array<{
    id: SocialProvider;
    label: string;
    iconLabel: string;
  }> = [
    { id: 'google', label: 'Continue with Google', iconLabel: 'G' },
    { id: 'facebook', label: 'Continue with Facebook', iconLabel: 'f' },
    { id: 'microsoft', label: 'Continue with Microsoft', iconLabel: 'M' },
  ];

  readonly form: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  ngOnInit(): void {
    const callbackError = this.route.snapshot.queryParamMap.get('error');
    const callbackCode = this.route.snapshot.queryParamMap.get('code');
    const callbackState = this.route.snapshot.queryParamMap.get('state');

    if (callbackError) {
      this.error.set('Social sign-in was cancelled or failed. Please try again.');
      return;
    }

    if (callbackCode && callbackState) {
      this.completeSocialSignIn(callbackCode, callbackState);
    }
  }

  async submit(): Promise<void> {
    this.submitted = true;
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    try {
      const username = String(this.form.controls['username'].value ?? '').trim();
      const password = String(this.form.controls['password'].value ?? '');
      await this.keycloak.authenticate(username, password);
      await this.router.navigate(['/jobs']);
    } catch (err) {
      const message = err instanceof Error ? err.message : '';
      if (message.toLowerCase().includes('invalid credentials')) {
        this.error.set('Invalid credentials. Please verify your identity and try again.');
      } else {
        this.error.set('Login failed. Please try again later.');
      }
    } finally {
      this.loading.set(false);
    }
  }

  async signInWith(provider: SocialProvider): Promise<void> {
    this.error.set(null);
    this.loadingSocial.set(provider);
    try {
      await this.keycloak.beginSocialLogin(provider);
    } catch (err) {
      const message = err instanceof Error ? err.message : '';
      this.error.set(message || 'Unable to start social sign-in. Please try again.');
      this.loadingSocial.set(null);
    }
  }

  private async completeSocialSignIn(code: string, state: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      await this.keycloak.completeSocialLogin(code, state);
      await this.router.navigate(['/jobs']);
    } catch (err) {
      const message = err instanceof Error ? err.message : '';
      this.error.set(message || 'Social sign-in failed. Please try again.');
      await this.router.navigate(['/auth/login']);
    } finally {
      this.loading.set(false);
      this.loadingSocial.set(null);
    }
  }
}
