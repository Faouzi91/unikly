import {
  AfterViewChecked,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, Subscription, debounceTime } from 'rxjs';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { MessageWebSocketService } from '../../../core/services/message-websocket.service';
import { MessageItem, MessagingService } from '../services/messaging.service';

@Component({
  selector: 'app-conversation',
  standalone: true,
  imports: [FormsModule, NgClass, RouterLink, TimeAgoPipe],
  templateUrl: './conversation.component.html',
  styleUrl: './conversation.component.scss',
})
export class ConversationComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('scrollAnchor') private scrollAnchor!: ElementRef<HTMLElement>;
  @ViewChild('topSentinel') private topSentinel!: ElementRef<HTMLElement>;
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLElement>;

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
    this.conversationId = this.route.snapshot.paramMap.get('id') ?? '';

    this.wsService.activate();
    this.subscribeToWebSocket();
    this.loadMessages(true);

    this.typingSub = this.typingSubject.pipe(debounceTime(500)).subscribe(() => {
      const recipientId = this.otherParticipantId();
      if (recipientId) {
        this.wsService.sendTypingIndicator(this.conversationId, recipientId);
      }
    });
  }

  ngAfterViewChecked(): void {
    if (!this.shouldScrollToBottom) return;
    this.scrollToBottom();
    this.shouldScrollToBottom = false;
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
      next: (message) => {
        this.messages.update((current) => [...current, message]);
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

    this.messagingService.getMessages(this.conversationId, this.currentPage, 50).subscribe({
      next: (page) => {
        if (initial) {
          if (page.content.length > 0) {
            const ids = new Set(page.content.map((item) => item.senderId).filter((id) => id !== this.currentUserId));
            const otherId = [...ids][0] ?? null;
            this.otherParticipantId.set(otherId);
            this.otherParticipantLabel.set(otherId ? `User ${otherId.slice(0, 8)}` : 'Conversation');
          }

          this.messages.set(page.content);
          this.loading.set(false);
          this.shouldScrollToBottom = true;
          this.markLastMessageRead();
          this.setupIntersectionObserver();
        } else {
          this.messages.update((current) => [...page.content, ...current]);
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
    this.wsMsgSub = this.wsService.messages$.subscribe((message) => {
      if (message.conversationId !== this.conversationId) return;
      this.zone.run(() => {
        this.messages.update((current) => [...current, message as MessageItem]);
        this.shouldScrollToBottom = true;
        this.messagingService.markAsRead(message.id).subscribe();
      });
    });

    this.wsTypingSub = this.wsService.typing$.subscribe((indicator) => {
      if (indicator.conversationId !== this.conversationId) return;
      this.zone.run(() => {
        this.isTyping.set(true);
        if (this.typingTimeout) clearTimeout(this.typingTimeout);
        this.typingTimeout = setTimeout(() => this.isTyping.set(false), 3000);
      });
    });
  }

  private markLastMessageRead(): void {
    const lastUnread = [...this.messages()]
      .reverse()
      .find((item) => item.senderId !== this.currentUserId && !item.readAt);

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
      { root: this.messagesContainer?.nativeElement, threshold: 0.1 },
    );
    this.observer.observe(this.topSentinel.nativeElement);
  }

  private scrollToBottom(): void {
    this.scrollAnchor?.nativeElement?.scrollIntoView({ behavior: 'smooth' });
  }
}
