import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { IncomingMessage, MessageWebSocketService } from '../../../core/services/message-websocket.service';
import { ConversationSummary, MessagingService } from '../services/messaging.service';

@Component({
  selector: 'app-conversation-list',
  standalone: true,
  imports: [RouterLink, TimeAgoPipe],
  templateUrl: './conversation-list.component.html',
  styleUrl: './conversation-list.component.scss',
})
export class ConversationListComponent implements OnInit, OnDestroy {
  private readonly messagingService = inject(MessagingService);
  private readonly wsService = inject(MessageWebSocketService);
  private readonly keycloak = inject(KeycloakService);

  readonly loading = signal(true);
  readonly conversations = signal<ConversationSummary[]>([]);
  readonly lastPreviewMap = signal<Record<string, string>>({});

  private currentUserId = '';
  private wsSub?: Subscription;

  ngOnInit(): void {
    this.currentUserId = this.keycloak.getUserId() ?? '';
    this.loadConversations();
    this.subscribeToWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  getOtherParticipantLabel(conversation: ConversationSummary): string {
    const otherId = conversation.participantIds.find((id) => id !== this.currentUserId);
    return otherId ? `User ${otherId.slice(0, 8)}` : 'Conversation';
  }

  private loadConversations(): void {
    this.loading.set(true);
    this.messagingService.getConversations(0).subscribe({
      next: (page) => {
        this.conversations.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private subscribeToWebSocket(): void {
    this.wsSub = this.wsService.messages$.subscribe((message: IncomingMessage) => {
      this.conversations.update((current) => {
        const index = current.findIndex((item) => item.id === message.conversationId);
        if (index < 0) return current;

        const updated = { ...current[index], lastMessageAt: message.createdAt };
        const rest = current.filter((_, itemIndex) => itemIndex !== index);
        return [updated, ...rest];
      });

      this.lastPreviewMap.update((map) => ({
        ...map,
        [message.conversationId]: message.content.slice(0, 80),
      }));
    });
  }
}
