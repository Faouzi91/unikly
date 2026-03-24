import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Observable, Subscription, tap } from 'rxjs';
import { KeycloakService } from '../auth/keycloak.service';
import { ApiService } from './api.service';
import { NotificationPayload, WebSocketService } from './websocket.service';

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  body: string;
  actionUrl?: string;
  createdAt: string;
  read: boolean;
}

export interface NotificationPage {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface NotificationPreferences {
  emailEnabled: boolean;
  pushEnabled: boolean;
  realtimeEnabled: boolean;
  quietHoursStart?: string;
  quietHoursEnd?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  readonly unreadCount = signal(0);
  readonly notifications = signal<NotificationItem[]>([]);

  private readonly api = inject(ApiService);
  private readonly ws = inject(WebSocketService);
  private readonly keycloak = inject(KeycloakService);

  private wsSub: Subscription | null = null;
  private initialized = false;

  init(): void {
    if (this.initialized || !this.keycloak.isAuthenticated()) return;
    this.initialized = true;
    this.loadInitialNotifications();
    this.subscribeToWebSocket();
    this.ws.activate();
  }

  getNotifications(page = 0, size = 20, unreadOnly = false): Observable<NotificationPage> {
    return this.api.get<NotificationPage>('/v1/notifications', {
      page,
      size,
      unread: unreadOnly,
    });
  }

  markAsRead(id: string): Observable<void> {
    return this.api.patch<void>(`/v1/notifications/${id}/read`).pipe(
      tap(() => {
        this.notifications.update((current) =>
          current.map((item) => (item.id === id ? { ...item, read: true } : item)),
        );
        this.unreadCount.update((count) => Math.max(0, count - 1));
      }),
    );
  }

  markAllRead(): Observable<void> {
    return this.api.patch<void>('/v1/notifications/read-all').pipe(
      tap(() => {
        this.notifications.update((current) => current.map((item) => ({ ...item, read: true })));
        this.unreadCount.set(0);
      }),
    );
  }

  getPreferences(): Observable<NotificationPreferences> {
    return this.api.get<NotificationPreferences>('/v1/notifications/preferences');
  }

  updatePreferences(prefs: NotificationPreferences): Observable<NotificationPreferences> {
    return this.api.put<NotificationPreferences>('/v1/notifications/preferences', prefs);
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.ws.deactivate();
  }

  private loadInitialNotifications(): void {
    this.api.get<NotificationPage>('/v1/notifications', { unread: true, page: 0, size: 20 }).subscribe({
      next: (page) => {
        this.notifications.set(page.content);
        this.unreadCount.set(page.content.filter((item) => !item.read).length);
      },
      error: () => {
        // Ignore boot-time failures; websocket updates will still hydrate the list.
      },
    });
  }

  private subscribeToWebSocket(): void {
    if (this.wsSub) return;
    this.wsSub = this.ws.notifications$.subscribe((incoming: NotificationPayload) => {
      this.notifications.update((current) => {
        const exists = current.some((item) => item.id === incoming.id);
        return exists ? current : [incoming, ...current];
      });
      if (!incoming.read) {
        this.unreadCount.update((count) => count + 1);
      }
    });
  }
}
