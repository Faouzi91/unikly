import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  readonly theme = inject(ThemeService);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal(false);
  submitted = false;

  readonly trustBadges = [
    { icon: '🔒', label: 'Escrow payments'  },
    { icon: '🤖', label: 'AI matching'      },
    { icon: '⚡', label: 'Same-day hire'    },
    { icon: '✅', label: 'Vetted talent'    },
  ];

  readonly form: FormGroup = this.fb.group({
    role:      ['FREELANCER', [Validators.required]],
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName:  ['', [Validators.required, Validators.maxLength(50)]],
    email:     ['', [Validators.required, Validators.email]],
    password:  ['', [Validators.required, Validators.minLength(8)]],
  });

  /** Returns 0–4 strength score based on password complexity. */
  passwordStrength(): number {
    const pw: string = this.form.controls['password'].value ?? '';
    if (pw.length < 8) return 0;
    let score = 1; // meets minimum length
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;
    return score;
  }

  strengthColor(): string {
    const s = this.passwordStrength();
    if (s <= 1) return 'bg-red-500';
    if (s === 2) return 'bg-amber-500';
    if (s === 3) return 'bg-yellow-400';
    return 'bg-accent';
  }

  strengthLabel(): { text: string; color: string } {
    const s = this.passwordStrength();
    if (s <= 1) return { text: 'Weak',   color: 'text-red-500'   };
    if (s === 2) return { text: 'Fair',   color: 'text-amber-500' };
    if (s === 3) return { text: 'Good',   color: 'text-yellow-500 dark:text-yellow-400' };
    return              { text: 'Strong', color: 'text-accent'    };
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
          this.error.set('Please check your details and try again.');
        } else {
          this.error.set('Registration failed. Please try again later.');
        }
      },
    });
  }
}
