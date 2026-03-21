import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { PublicProfileComponent } from './public-profile.component';
import { UserService } from '../services/user.service';
import { MessagingService } from '../../messaging/services/messaging.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { of } from 'rxjs';

describe('PublicProfileComponent', () => {
  let component: PublicProfileComponent;
  let fixture: ComponentFixture<PublicProfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicProfileComponent, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'test-id' } } } },
        { provide: UserService, useValue: { getProfile: () => of(null), getReviews: () => of({ content: [], totalElements: 0 }) } },
        { provide: MessagingService, useValue: { getOrCreateConversation: () => of({ id: '1' }) } },
        { provide: KeycloakService, useValue: { getUserId: () => 'test', login: () => {} } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PublicProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
