import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginComponent } from './login.component';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { ThemeService } from '../../../core/services/theme.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let router: Router;

  const keycloakServiceSpy = jasmine.createSpyObj<KeycloakService>('KeycloakService', [
    'authenticate',
    'isAuthenticated',
    'beginSocialLogin',
    'completeSocialLogin',
  ]);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, RouterTestingModule],
      providers: [
        ThemeService,
        { provide: KeycloakService, useValue: keycloakServiceSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  beforeEach(() => {
    keycloakServiceSpy.authenticate.calls.reset();
    keycloakServiceSpy.authenticate.and.resolveTo();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have an invalid form when empty', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('should be valid when username and password are provided', () => {
    component.form.setValue({ username: 'user@test.com', password: 'password123' });
    expect(component.form.valid).toBeTrue();
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword()).toBeFalse();
    component.showPassword.set(true);
    expect(component.showPassword()).toBeTrue();
  });

  it('should not submit when form is invalid', async () => {
    await component.submit();
    expect(keycloakServiceSpy.authenticate).not.toHaveBeenCalled();
  });

  it('should authenticate and navigate to jobs', async () => {
    component.form.setValue({ username: 'user@test.com', password: 'password123' });

    await component.submit();

    expect(keycloakServiceSpy.authenticate).toHaveBeenCalledWith('user@test.com', 'password123');
    expect(router.navigate).toHaveBeenCalledWith(['/jobs']);
  });

  it('should show invalid credentials message', async () => {
    keycloakServiceSpy.authenticate.and.rejectWith(new Error('Invalid credentials'));
    component.form.setValue({ username: 'user@test.com', password: 'password123' });

    await component.submit();

    expect(component.error()).toContain('Invalid credentials');
    expect(component.loading()).toBeFalse();
  });
});
