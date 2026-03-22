import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy, inject } from '@angular/core';
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

  private readonly wsBaseUrl = environment.wsUrl.replace('ws://', 'http://').replace('wss://', 'https://');

  constructor() {
    this.client = new Client({ reconnectDelay: 2000 });
  }

  async activate(): Promise<void> {
    const token = await this.keycloak.getToken();

    this.client.configure({
      brokerURL: `${window.location.origin.replace('http', 'ws')}${environment.wsUrl}/notifications`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      onConnect: () => {
        this.stopPolling();
        this.client.subscribe('/user/queue/notifications', (message: IMessage) => {
          try {
            const notification = JSON.parse(message.body) as NotificationPayload;
            this.notifications$.next(notification);
          } catch {
            console.error('Failed to parse notification payload', message.body);
          }
        });
      },
      onDisconnect: () => this.startPolling(),
      onStompError: () => this.startPolling(),
    });

    this.client.activate();
  }

  deactivate(): void {
    this.stopPolling();
    if (this.client.active) {
      this.client.deactivate();
    }
  }

  ngOnDestroy(): void {
    this.deactivate();
    this.notifications$.complete();
  }

  private startPolling(): void {
    if (this.pollingInterval) return;
    const apiUrl = this.wsBaseUrl.replace('/ws', '') + '/api/v1/notifications?unread=true&page=0&size=10';

    this.pollingInterval = setInterval(() => {
      this.http.get<{ content: NotificationPayload[] }>(apiUrl).subscribe({
        next: (page) => page.content.forEach((item) => this.notifications$.next(item)),
        error: () => {
          // Silent retry fallback while websocket is reconnecting.
        },
      });
    }, 30_000);
  }

  private stopPolling(): void {
    if (!this.pollingInterval) return;
    clearInterval(this.pollingInterval);
    this.pollingInterval = null;
  }
}
