import { Injectable, OnDestroy, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, IMessage } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { KeycloakService } from '../auth/keycloak.service';

export interface NotificationPayload {
  id: string;
  type: string;
  title: string;
  body: string;
  actionUrl?: string;
  createdAt: string;
  read: boolean;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  readonly notifications$ = new Subject<NotificationPayload>();

  private readonly keycloak = inject(KeycloakService);
  private readonly http = inject(HttpClient);

  private client: Client;
  private pollingInterval: ReturnType<typeof setInterval> | null = null;

  private readonly wsBaseUrl = environment.wsUrl
    .replace('ws://', 'http://')
    .replace('wss://', 'https://');

  constructor() {
    this.client = new Client({
      reconnectDelay: 2000,
    });
  }

  async activate(): Promise<void> {
    const token = await this.keycloak.getToken();

    this.client.configure({
      brokerURL: `${environment.wsUrl}/notifications/websocket`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      onConnect: () => {
        console.log('WebSocket connected');
        this.stopPolling();
        this.client.subscribe('/user/queue/notifications', (msg: IMessage) => {
          try {
            const notification = JSON.parse(msg.body) as NotificationPayload;
            this.notifications$.next(notification);
          } catch {
            console.error('Failed to parse notification payload', msg.body);
          }
        });
      },
      onDisconnect: () => {
        this.startPolling();
      },
      onStompError: () => {
        this.startPolling();
      },
    });

    this.client.activate();
  }

  deactivate(): void {
    this.stopPolling();
    if (this.client.active) {
      this.client.deactivate();
    }
  }

  private startPolling(): void {
    if (this.pollingInterval) return;
    const apiUrl = this.wsBaseUrl.replace('/ws', '') + '/api/v1/notifications?unread=true&page=0&size=10';
    this.pollingInterval = setInterval(() => {
      this.http.get<{ content: NotificationPayload[] }>(apiUrl).subscribe({
        next: (page) => page.content.forEach((n) => this.notifications$.next(n)),
        error: () => { /* silent — will retry on next tick */ },
      });
    }, 30_000);
  }

  private stopPolling(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
  }

  ngOnDestroy(): void {
    this.deactivate();
    this.notifications$.complete();
  }
}
