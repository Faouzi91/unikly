import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { IncomingMessage, MessageWebSocketService } from '../../../core/services/message-websocket.service';
import { ConversationSummary, MessagingService } from '../services/messaging.service';
import { UserService } from '../../profile/services/user.service';

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
  private readonly userService = inject(UserService);

  readonly loading = signal(true);
  readonly conversations = signal<ConversationSummary[]>([]);
  readonly participantNames = signal<Record<string, string>>({});
  readonly participantInitials = signal<Record<string, string>>({});
  readonly onlineStatus = signal<Record<string, boolean>>({});

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

  getOtherId(conversation: ConversationSummary): string | null {
    return conversation.participantIds.find((id) => id !== this.currentUserId) ?? null;
  }

  getOtherParticipantLabel(conversation: ConversationSummary): string {
    const otherId = this.getOtherId(conversation);
    if (!otherId) return 'Conversation';
    return this.participantNames()[otherId] ?? `User ${otherId.slice(0, 6)}…`;
  }

  getOtherParticipantInitials(conversation: ConversationSummary): string {
    const otherId = this.getOtherId(conversation);
    if (!otherId) return '?';
    return this.participantInitials()[otherId] ?? '?';
  }

  isOnline(conversation: ConversationSummary): boolean {
    const otherId = this.getOtherId(conversation);
    return otherId ? this.onlineStatus()[otherId] === true : false;
  }

  getPreview(conversation: ConversationSummary): string {
    return conversation.lastMessagePreview ?? 'No messages yet';
  }

  private loadConversations(): void {
    this.loading.set(true);
    this.messagingService.getConversations(0).subscribe({
      next: (page) => {
        this.conversations.set(page.content);
        this.loading.set(false);
        this.resolveParticipantNames(page.content);
        this.loadPresence(page.content);
      },
      error: () => this.loading.set(false),
    });
  }

  private resolveParticipantNames(conversations: ConversationSummary[]): void {
    const otherIds = new Set<string>();
    for (const conv of conversations) {
      const otherId = this.getOtherId(conv);
      if (otherId) otherIds.add(otherId);
    }
    for (const id of otherIds) {
      this.userService.getProfile(id).subscribe({
        next: (profile) => {
          const name = profile.displayName;
          const initials = name.trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase();
          this.participantNames.update((map) => ({ ...map, [id]: name }));
          this.participantInitials.update((map) => ({ ...map, [id]: initials || '?' }));
        },
      });
    }
  }

  private loadPresence(conversations: ConversationSummary[]): void {
    const otherIds = conversations
      .map((c) => this.getOtherId(c))
      .filter((id): id is string => !!id);
    if (otherIds.length === 0) return;
    this.messagingService.getPresence(otherIds).subscribe({
      next: (status) => this.onlineStatus.set(status),
    });
  }

  private subscribeToWebSocket(): void {
    this.wsSub = this.wsService.messages$.subscribe((message: IncomingMessage) => {
      this.conversations.update((current) => {
        const index = current.findIndex((item) => item.id === message.conversationId);
        if (index < 0) return current;
        const preview = message.content.length > 80 ? message.content.slice(0, 80) + '…' : message.content;
        const updated = {
          ...current[index],
          lastMessageAt: message.createdAt,
          lastMessagePreview: preview,
          unreadCount: current[index].unreadCount + 1,
        };
        const rest = current.filter((_, i) => i !== index);
        return [updated, ...rest];
      });
    });
  }
}
