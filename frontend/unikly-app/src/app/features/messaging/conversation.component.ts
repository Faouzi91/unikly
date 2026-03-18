import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  AfterViewChecked,
  ElementRef,
  ViewChild,
  NgZone,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { Subscription, Subject, debounceTime } from 'rxjs';

import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TextFieldModule } from '@angular/cdk/text-field';

import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';
import { MessagingService, MessageItem } from './services/messaging.service';
import { MessageWebSocketService } from '../../core/services/message-websocket.service';
import { KeycloakService } from '../../core/auth/keycloak.service';

@Component({
  selector: 'app-conversation',
  standalone: true,
  imports: [
    FormsModule,
    NgClass,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    TextFieldModule,
    TimeAgoPipe,
  ],
  styles: [`
    .typing-dot {
      display: inline-block;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #6b7280;
      animation: bounce 1.2s ease-in-out infinite;
    }
    .typing-dot:nth-child(2) { animation-delay: 0.2s; }
    .typing-dot:nth-child(3) { animation-delay: 0.4s; }
    @keyframes bounce {
      0%, 80%, 100% { transform: translateY(0); }
      40% { transform: translateY(-6px); }
    }
  `],
  template: `
    <div class="flex flex-col h-[calc(100vh-64px)]">

      <!-- Header -->
      <div class="flex items-center gap-3 px-4 py-3 border-b bg-white shadow-sm">
        <button mat-icon-button routerLink="/messages">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <div class="flex-1">
          <p class="font-semibold">{{ otherParticipantLabel() }}</p>
        </div>
        @if (otherParticipantId()) {
          <a mat-button [routerLink]="['/users', otherParticipantId()]" color="primary">
            View Profile
          </a>
        }
      </div>

      <!-- Top sentinel for infinite scroll -->
      <div #topSentinel class="h-1"></div>

      <!-- Messages area -->
      <div
        #messagesContainer
        class="flex-1 overflow-y-auto px-4 py-3 space-y-2"
      >
        @if (loadingMore()) {
          <div class="flex justify-center py-2">
            <mat-spinner diameter="24" />
          </div>
        }

        @for (msg of messages(); track msg.id) {
          <div
            class="flex"
            [ngClass]="msg.senderId === currentUserId ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-xs lg:max-w-md px-4 py-2 rounded-2xl text-sm"
              [ngClass]="msg.senderId === currentUserId
                ? 'bg-indigo-600 text-white rounded-br-sm'
                : 'bg-gray-100 text-gray-800 rounded-bl-sm'"
            >
              <p class="whitespace-pre-wrap break-words">{{ msg.content }}</p>
              <div
                class="flex items-center gap-1 mt-1"
                [ngClass]="msg.senderId === currentUserId ? 'justify-end' : 'justify-start'"
              >
                <span class="text-xs opacity-60">{{ msg.createdAt | timeAgo }}</span>
                @if (msg.senderId === currentUserId && msg.readAt) {
                  <mat-icon
                    class="opacity-70"
                    style="font-size:14px;width:14px;height:14px;line-height:14px"
                  >done_all</mat-icon>
                }
              </div>
            </div>
          </div>
        }

        <!-- Typing indicator -->
        @if (isTyping()) {
          <div class="flex justify-start">
            <div class="bg-gray-100 px-4 py-3 rounded-2xl rounded-bl-sm flex items-center gap-1">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </div>
        }

        <!-- Scroll anchor -->
        <div #scrollAnchor></div>
      </div>

      <!-- Input area -->
      <div class="border-t bg-white px-4 py-3 flex items-end gap-2">
        <mat-form-field class="flex-1" appearance="outline" subscriptSizing="dynamic">
          <textarea
            matInput
            [(ngModel)]="newMessage"
            placeholder="Type a message..."
            rows="1"
            cdkTextareaAutosize
            (keydown)="onKeydown($event)"
            (input)="onTyping()"
            class="resize-none"
          ></textarea>
        </mat-form-field>
        <button
          mat-icon-button
          color="primary"
          [disabled]="!newMessage.trim() || sending()"
          (click)="sendMessage()"
          class="mb-1"
        >
          <mat-icon>send</mat-icon>
        </button>
      </div>
    </div>
  `,
})
export class ConversationComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('scrollAnchor') private scrollAnchor!: ElementRef;
  @ViewChild('topSentinel') private topSentinel!: ElementRef;
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  private readonly route = inject(ActivatedRoute);
  private readonly messagingService = inject(MessagingService);
  private readonly wsService = inject(MessageWebSocketService);
  private readonly keycloak = inject(KeycloakService);
  private readonly zone = inject(NgZone);

  readonly messages = signal<MessageItem[]>([]);
  readonly loading = signal(true);
  readonly loadingMore = signal(false);
  readonly sending = signal(false);
  readonly isTyping = signal(false);
  readonly otherParticipantLabel = signal('Conversation');
  readonly otherParticipantId = signal<string | null>(null);

  currentUserId = '';
  newMessage = '';

  private conversationId = '';
  private currentPage = 0;
  private hasMorePages = false;
  private shouldScrollToBottom = false;
  private typingTimeout: ReturnType<typeof setTimeout> | null = null;
  private readonly typingSubject = new Subject<void>();
  private wsMsgSub?: Subscription;
  private wsTypingSub?: Subscription;
  private typingSub?: Subscription;
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    this.currentUserId = this.keycloak.getUserId() ?? '';
    this.conversationId = this.route.snapshot.paramMap.get('id')!;

    this.wsService.activate();
    this.subscribeToWebSocket();
    this.loadMessages(true);

    this.typingSub = this.typingSubject
      .pipe(debounceTime(500))
      .subscribe(() => {
        const otherId = this.otherParticipantId();
        if (otherId) {
          this.wsService.sendTypingIndicator(this.conversationId, otherId);
        }
      });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  ngOnDestroy(): void {
    this.wsMsgSub?.unsubscribe();
    this.wsTypingSub?.unsubscribe();
    this.typingSub?.unsubscribe();
    this.observer?.disconnect();
    if (this.typingTimeout) clearTimeout(this.typingTimeout);
    this.typingSubject.complete();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  onTyping(): void {
    this.typingSubject.next();
  }

  sendMessage(): void {
    const content = this.newMessage.trim();
    if (!content || this.sending()) return;

    this.sending.set(true);
    this.newMessage = '';

    this.messagingService.sendMessage(this.conversationId, content).subscribe({
      next: (msg) => {
        this.messages.update((list) => [...list, msg]);
        this.shouldScrollToBottom = true;
        this.sending.set(false);
      },
      error: () => {
        this.newMessage = content;
        this.sending.set(false);
      },
    });
  }

  private loadMessages(initial: boolean): void {
    if (initial) {
      this.loading.set(true);
      this.currentPage = 0;
    } else {
      this.loadingMore.set(true);
    }

    this.messagingService
      .getMessages(this.conversationId, this.currentPage, 50)
      .subscribe({
        next: (page) => {
          if (initial) {
            const conversation = this.route.snapshot.data['conversation'];
            if (page.content.length > 0) {
              const otherIds = new Set(
                page.content
                  .map((m) => m.senderId)
                  .filter((id) => id !== this.currentUserId)
              );
              const otherId = [...otherIds][0] ?? null;
              this.otherParticipantId.set(otherId);
              this.otherParticipantLabel.set(
                otherId ? `User ${otherId.slice(0, 8)}…` : 'Conversation'
              );
            }
            this.messages.set(page.content);
            this.shouldScrollToBottom = true;
            this.loading.set(false);
            this.markLastMessageRead();
            this.setupIntersectionObserver();
          } else {
            this.messages.update((list) => [...page.content, ...list]);
            this.loadingMore.set(false);
          }
          this.hasMorePages = page.number < page.totalPages - 1;
        },
        error: () => {
          this.loading.set(false);
          this.loadingMore.set(false);
        },
      });
  }

  private loadOlderMessages(): void {
    if (!this.hasMorePages || this.loadingMore()) return;
    this.currentPage++;
    this.loadMessages(false);
  }

  private subscribeToWebSocket(): void {
    this.wsMsgSub = this.wsService.messages$.subscribe((msg) => {
      if (msg.conversationId !== this.conversationId) return;
      this.zone.run(() => {
        this.messages.update((list) => [...list, msg as MessageItem]);
        this.shouldScrollToBottom = true;
        this.messagingService.markAsRead(msg.id).subscribe();
      });
    });

    this.wsTypingSub = this.wsService.typing$.subscribe((indicator) => {
      if (indicator.conversationId !== this.conversationId) return;
      this.zone.run(() => {
        this.isTyping.set(true);
        if (this.typingTimeout) clearTimeout(this.typingTimeout);
        this.typingTimeout = setTimeout(
          () => this.isTyping.set(false),
          3000
        );
      });
    });
  }

  private markLastMessageRead(): void {
    const msgs = this.messages();
    const lastUnread = [...msgs]
      .reverse()
      .find((m) => m.senderId !== this.currentUserId && !m.readAt);
    if (lastUnread) {
      this.messagingService.markAsRead(lastUnread.id).subscribe();
    }
  }

  private setupIntersectionObserver(): void {
    if (!this.topSentinel?.nativeElement) return;
    this.observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          this.zone.run(() => this.loadOlderMessages());
        }
      },
      { root: this.messagesContainer?.nativeElement, threshold: 0.1 }
    );
    this.observer.observe(this.topSentinel.nativeElement);
  }

  private scrollToBottom(): void {
    try {
      this.scrollAnchor?.nativeElement?.scrollIntoView({ behavior: 'smooth' });
    } catch {
      // ignore
    }
  }
}
