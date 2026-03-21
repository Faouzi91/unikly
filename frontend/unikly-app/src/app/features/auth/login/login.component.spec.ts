import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { LoginComponent } from './login.component';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { ThemeService } from '../../../core/services/theme.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  const keycloakServiceSpy = jasmine.createSpyObj<KeycloakService>('KeycloakService', [
    'getTokenWithPassword',
    'isAuthenticated',
  ]);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule, RouterTestingModule],
      providers: [
        ThemeService,
        { provide: KeycloakService, useValue: keycloakServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
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

  it('should show password toggle working', () => {
    expect(component.showPassword()).toBeFalse();
    component.showPassword.set(true);
    expect(component.showPassword()).toBeTrue();
  });

  it('should not submit when form is invalid', async () => {
    await component.submit();
    expect(keycloakServiceSpy.getTokenWithPassword).not.toHaveBeenCalled();
  });
});
