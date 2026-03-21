import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';

import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { MessagingService, ConversationSummary } from '../services/messaging.service';
import { MessageWebSocketService, IncomingMessage } from '../../../core/services/message-websocket.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';

@Component({
  selector: 'app-conversation-list',
  standalone: true,
  imports: [
    RouterLink,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    TimeAgoPipe,
  ],
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
    this.subscribeToWs();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  getOtherParticipantLabel(conv: ConversationSummary): string {
    const other = conv.participantIds.find((id) => id !== this.currentUserId);
    return other ? `User ${other.slice(0, 8)}…` : 'Unknown';
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

  private subscribeToWs(): void {
    this.wsSub = this.wsService.messages$.subscribe((msg: IncomingMessage) => {
      this.conversations.update((list) => {
        const idx = list.findIndex((c) => c.id === msg.conversationId);
        if (idx === -1) return list;

        const updated = { ...list[idx], lastMessageAt: msg.createdAt };
        const rest = list.filter((_, i) => i !== idx);
        return [updated, ...rest];
      });

      this.lastPreviewMap.update((map) => ({
        ...map,
        [msg.conversationId]: msg.content.slice(0, 50),
      }));
    });
  }
}
