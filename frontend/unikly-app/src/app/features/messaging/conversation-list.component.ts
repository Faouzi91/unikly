import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';

import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';
import { MessagingService, ConversationSummary } from './services/messaging.service';
import { MessageWebSocketService, IncomingMessage } from '../../core/services/message-websocket.service';
import { KeycloakService } from '../../core/auth/keycloak.service';

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
  template: `
    <div class="p-6 max-w-2xl mx-auto">
      <h1 class="text-2xl font-bold mb-6">Messages</h1>

      @if (loading()) {
        <div class="flex justify-center py-12">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
        </div>
      } @else if (conversations().length === 0) {
        <div class="text-center py-16">
          <mat-icon class="text-6xl text-gray-300 mb-4" style="font-size:64px;width:64px;height:64px">chat</mat-icon>
          <p class="text-gray-500 text-lg">No conversations yet.</p>
          <p class="text-gray-400 text-sm mt-1">Start one from a freelancer's profile!</p>
        </div>
      } @else {
        <mat-nav-list>
          @for (conv of conversations(); track conv.id) {
            <a
              mat-list-item
              [routerLink]="['/messages', conv.id]"
              class="rounded-lg mb-1 hover:!bg-indigo-50"
            >
              <mat-icon matListItemIcon class="text-indigo-400">chat_bubble_outline</mat-icon>
              <div matListItemTitle class="font-medium">
                {{ getOtherParticipantLabel(conv) }}
              </div>
              <div matListItemLine class="text-sm text-gray-500">
                {{ lastPreviewMap()[conv.id] || 'No messages yet' }}
              </div>
              <span matListItemMeta class="text-xs text-gray-400">
                {{ conv.lastMessageAt | timeAgo }}
              </span>
            </a>
            <mat-divider />
          }
        </mat-nav-list>
      }
    </div>
  `,
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
