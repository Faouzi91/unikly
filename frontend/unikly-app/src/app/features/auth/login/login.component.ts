import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  readonly theme = inject(ThemeService);
  private readonly fb = inject(FormBuilder);
  private readonly keycloak = inject(KeycloakService);
  private readonly router = inject(Router);

  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  submitted = false;

  readonly metrics = [
    { value: '12k+', label: 'Experts'  },
    { value: '98%',  label: 'Accuracy' },
    { value: '4.9★', label: 'Rating'   },
  ];

  readonly form: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  async submit(): Promise<void> {
    this.submitted = true;
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    try {
      const { username, password } = this.form.value;
      await this.keycloak.getTokenWithPassword(username, password);
      await this.router.navigate(['/jobs']);
    } catch {
      this.error.set('Invalid credentials. Please verify your identity and try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
