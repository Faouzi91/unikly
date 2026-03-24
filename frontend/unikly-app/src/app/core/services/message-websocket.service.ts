import { Injectable, OnDestroy, inject, NgZone } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { KeycloakService } from '../auth/keycloak.service';

export interface IncomingMessage {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  contentType: string;
  readAt: string | null;
  createdAt: string;
}

export interface TypingIndicator {
  conversationId: string;
  senderId: string;
}

@Injectable({ providedIn: 'root' })
export class MessageWebSocketService implements OnDestroy {
  readonly messages$ = new Subject<IncomingMessage>();
  readonly typing$ = new Subject<TypingIndicator>();

  private readonly keycloak = inject(KeycloakService);
  private readonly zone = inject(NgZone);
  private client: Client;

  constructor() {
    this.client = new Client({ reconnectDelay: 2000 });
  }

  async activate(): Promise<void> {
    const token = await this.keycloak.getToken();

    this.client.configure({
      brokerURL: `${window.location.origin.replace('http', 'ws')}${environment.wsUrl}/messages`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      onConnect: () => {
        this.client.subscribe('/user/queue/messages', (msg: IMessage) => {
          try {
            const message = JSON.parse(msg.body) as IncomingMessage;
            this.zone.run(() => this.messages$.next(message));
          } catch {
            console.error('Failed to parse incoming message', msg.body);
          }
        });

        this.client.subscribe('/user/queue/typing', (msg: IMessage) => {
          try {
            const indicator = JSON.parse(msg.body) as TypingIndicator;
            this.zone.run(() => this.typing$.next(indicator));
          } catch {
            console.error('Failed to parse typing indicator', msg.body);
          }
        });
      },
    });

    this.client.activate();
  }

  deactivate(): void {
    if (this.client.active) {
      this.client.deactivate();
    }
  }

  sendTypingIndicator(conversationId: string, recipientId: string): void {
    if (!this.client.active) return;
    this.client.publish({
      destination: '/app/typing',
      body: JSON.stringify({ conversationId, recipientId }),
    });
  }

  ngOnDestroy(): void {
    this.deactivate();
    this.messages$.complete();
    this.typing$.complete();
  }
}
