import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { ConversationComponent } from './conversation.component';
import { MessagingService } from '../services/messaging.service';
import { MessageWebSocketService } from '../../../core/services/message-websocket.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { of, Subject } from 'rxjs';

describe('ConversationComponent', () => {
  let component: ConversationComponent;
  let fixture: ComponentFixture<ConversationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConversationComponent, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' }, data: {} } } },
        { provide: MessagingService, useValue: { getMessages: () => of({ content: [], number: 0, totalPages: 1 }), markAsRead: () => of({}) } },
        { provide: MessageWebSocketService, useValue: { messages$: new Subject(), typing$: new Subject(), activate: () => {}, sendTypingIndicator: () => {} } },
        { provide: KeycloakService, useValue: { getUserId: () => 'test' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ConversationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
