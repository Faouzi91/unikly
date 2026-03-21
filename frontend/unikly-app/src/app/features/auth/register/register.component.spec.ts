import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RegisterComponent } from './register.component';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,
        ReactiveFormsModule,
        RouterTestingModule,
        HttpClientTestingModule,
      ],
      providers: [ThemeService, AuthService],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default role to FREELANCER', () => {
    expect(component.form.get('role')?.value).toBe('FREELANCER');
  });

  it('should be invalid when empty', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('should be valid when all required fields are filled', () => {
    component.form.patchValue({
      role: 'CLIENT',
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@example.com',
      password: 'SecurePass1!',
    });
    expect(component.form.valid).toBeTrue();
  });

  it('should compute password strength correctly', () => {
    component.form.get('password')?.setValue('abc'); // short, weak
    expect(component.passwordStrength()).toBe(0);

    component.form.get('password')?.setValue('Abcdefgh'); // 8 chars + uppercase
    expect(component.passwordStrength()).toBeGreaterThanOrEqual(2);
  });

  it('should not call authService.register when form is invalid', () => {
    component.submit();
    expect(component.success()).toBeFalse();
  });
});
