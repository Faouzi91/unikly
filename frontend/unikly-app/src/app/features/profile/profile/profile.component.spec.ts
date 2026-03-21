import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ProfileComponent } from './profile.component';
import { UserService } from '../services/user.service';
import { JobService } from '../../jobs/services/job.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { of } from 'rxjs';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileComponent, RouterTestingModule],
      providers: [
        { provide: UserService, useValue: { getMyProfile: () => of(null), getReviews: () => of({ content: [], totalElements: 0 }), updateMyProfile: () => of(null) } },
        { provide: JobService, useValue: { getSuggestions: () => of([]) } },
        { provide: KeycloakService, useValue: { hasRole: () => false, getUserId: () => 'test' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
