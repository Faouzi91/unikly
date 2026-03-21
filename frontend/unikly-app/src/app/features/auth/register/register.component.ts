import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';
import { KeycloakService, SocialProvider } from '../../../core/auth/keycloak.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent implements OnInit {
  readonly theme = inject(ThemeService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly keycloak = inject(KeycloakService);

  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly loadingSocial = signal<SocialProvider | null>(null);
  readonly error = signal<string | null>(null);
  readonly success = signal(false);
  submitted = false;

  readonly trustBadges = [
    'Escrow-protected contracts',
    'AI-assisted candidate matching',
    'Verified freelancer and client identities',
    'Secure milestones and messaging',
  ];

  readonly socialProviders: Array<{ id: SocialProvider; label: string }> = [
    { id: 'google', label: 'Google' },
    { id: 'facebook', label: 'Facebook' },
    { id: 'microsoft', label: 'Microsoft' },
  ];

  readonly form: FormGroup = this.fb.group({
    role: ['FREELANCER', [Validators.required]],
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    const preferredRole = this.route.snapshot.queryParamMap.get('role')?.toLowerCase().trim();
    if (preferredRole === 'client') {
      this.form.patchValue({ role: 'CLIENT' });
    }
    if (preferredRole === 'freelancer') {
      this.form.patchValue({ role: 'FREELANCER' });
    }
  }

  passwordStrength(): number {
    const pw: string = this.form.controls['password'].value ?? '';
    if (pw.length < 8) return 0;

    let score = 1;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;
    return score;
  }

  strengthColor(): string {
    const score = this.passwordStrength();
    if (score <= 1) return 'bg-red-500';
    if (score === 2) return 'bg-amber-500';
    if (score === 3) return 'bg-yellow-500';
    return 'bg-brand';
  }

  strengthLabel(): { text: string; color: string } {
    const score = this.passwordStrength();
    if (score <= 1) return { text: 'Weak', color: 'text-red-600' };
    if (score === 2) return { text: 'Fair', color: 'text-amber-600' };
    if (score === 3) return { text: 'Good', color: 'text-yellow-600' };
    return { text: 'Strong', color: 'text-brand-dark' };
  }

  submit(): void {
    this.submitted = true;
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { role, firstName, lastName, email, password } = this.form.value;
    this.authService.register({ firstName, lastName, email, password, role }).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 409) {
          this.error.set('An account with this email already exists.');
        } else if (err.status === 400) {
          this.error.set('Please verify your details and try again.');
        } else {
          this.error.set('Registration failed. Please try again later.');
        }
      },
    });
  }

  async signUpWith(provider: SocialProvider): Promise<void> {
    this.error.set(null);
    this.loadingSocial.set(provider);

    try {
      await this.keycloak.beginSocialLogin(provider);
    } catch (err) {
      const message = err instanceof Error ? err.message : '';
      this.error.set(message || 'Unable to start social sign-up. Please try again.');
      this.loadingSocial.set(null);
    }
  }
}
