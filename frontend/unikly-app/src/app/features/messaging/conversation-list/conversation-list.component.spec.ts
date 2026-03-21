import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ConversationListComponent } from './conversation-list.component';
import { MessagingService } from '../services/messaging.service';
import { MessageWebSocketService } from '../../../core/services/message-websocket.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { of, Subject } from 'rxjs';

describe('ConversationListComponent', () => {
  let component: ConversationListComponent;
  let fixture: ComponentFixture<ConversationListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConversationListComponent, RouterTestingModule],
      providers: [
        { provide: MessagingService, useValue: { getConversations: () => of({ content: [] }) } },
        { provide: MessageWebSocketService, useValue: { messages$: new Subject() } },
        { provide: KeycloakService, useValue: { getUserId: () => 'test' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ConversationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
