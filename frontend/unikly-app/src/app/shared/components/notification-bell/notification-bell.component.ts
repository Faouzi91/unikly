import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { Router } from '@angular/router';
import { NgClass } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { TimeAgoPipe } from '../../pipes/time-ago.pipe';
import {
  NotificationService,
  NotificationItem,
} from '../../../core/services/notification.service';

const TYPE_ICONS: Record<string, string> = {
  JOB_MATCHED: 'work',
  PROPOSAL_RECEIVED: 'description',
  PROPOSAL_ACCEPTED: 'check_circle',
  PAYMENT_FUNDED: 'payments',
  ESCROW_RELEASED: 'account_balance_wallet',
  MESSAGE_RECEIVED: 'chat',
  SYSTEM: 'info',
};

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [
    NgClass,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatMenuModule,
    MatDividerModule,
    TimeAgoPipe,
  ],
  template: `
    <button
      mat-icon-button
      [matMenuTriggerFor]="notifMenu"
      [ngClass]="{ 'bell-pulse': isPulsing() }"
      aria-label="Notifications"
    >
      <mat-icon
        [matBadge]="unreadCount() || null"
        matBadgeColor="warn"
        [matBadgeHidden]="unreadCount() === 0"
      >notifications</mat-icon>
    </button>

    <mat-menu #notifMenu="matMenu" class="notif-menu-panel" xPosition="before">
      <!-- Header -->
      <div class="flex items-center justify-between px-4 py-2" (click)="$event.stopPropagation()">
        <span class="font-semibold text-base">Notifications</span>
        <button
          mat-button
          color="primary"
          class="text-xs"
          [disabled]="unreadCount() === 0"
          (click)="onMarkAllRead()"
        >Mark all read</button>
      </div>

      <mat-divider />

      <!-- Empty state -->
      @if (recent().length === 0) {
        <div class="px-4 py-6 text-center text-gray-400 text-sm">
          <mat-icon class="text-gray-300 !text-4xl block mb-2">notifications_none</mat-icon>
          No notifications yet
        </div>
      }

      <!-- Notification items -->
      @for (n of recent(); track n.id) {
        <button
          mat-menu-item
          class="notif-item"
          [ngClass]="{ 'bg-blue-50': !n.read }"
          (click)="onItemClick(n)"
        >
          <div class="flex items-start gap-3 py-1 w-full">
            <mat-icon class="text-blue-500 shrink-0 mt-0.5">{{ iconFor(n.type) }}</mat-icon>
            <div class="flex flex-col min-w-0">
              <span class="font-medium text-sm leading-tight truncate max-w-56">{{ n.title }}</span>
              <span class="text-xs text-gray-500 leading-snug line-clamp-2">{{ n.body }}</span>
              <span class="text-xs text-gray-400 mt-0.5">{{ n.createdAt | timeAgo }}</span>
            </div>
            @if (!n.read) {
              <span class="w-2 h-2 rounded-full bg-blue-500 shrink-0 mt-1.5 ml-auto"></span>
            }
          </div>
        </button>
      }

      <mat-divider />

      <!-- Footer -->
      <div class="px-4 py-2" (click)="$event.stopPropagation()">
        <button mat-button color="primary" class="w-full text-sm" (click)="viewAll()">
          View all notifications
        </button>
      </div>
    </mat-menu>
  `,
  styles: [`
    @keyframes bell-pulse {
      0%   { transform: scale(1); }
      30%  { transform: scale(1.25) rotate(-10deg); }
      60%  { transform: scale(1.15) rotate(10deg); }
      100% { transform: scale(1); }
    }
    .bell-pulse ::ng-deep .mat-icon {
      animation: bell-pulse 0.6s ease;
    }
    .notif-item {
      height: auto !important;
      min-height: 56px;
      white-space: normal !important;
    }
  `],
})
export class NotificationBellComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly unreadCount = this.notificationService.unreadCount;
  readonly recent = computed(() => this.notificationService.notifications().slice(0, 10));

  readonly isPulsing = signal(false);

  private previousUnread = 0;

  ngOnInit(): void {
    this.notificationService.init();
    this.watchForNewNotifications();
  }

  private watchForNewNotifications(): void {
    // Poll the signal for changes — effect() not needed; we track in getter.
    // We use setInterval as a lightweight change detector for the pulse animation.
    setInterval(() => {
      const current = this.unreadCount();
      if (current > this.previousUnread) {
        this.triggerPulse();
      }
      this.previousUnread = current;
    }, 500);
  }

  private triggerPulse(): void {
    this.isPulsing.set(true);
    setTimeout(() => this.isPulsing.set(false), 700);
  }

  iconFor(type: string): string {
    return TYPE_ICONS[type] ?? 'notifications';
  }

  onItemClick(n: NotificationItem): void {
    if (!n.read) {
      this.notificationService.markAsRead(n.id).subscribe();
    }
    if (n.actionUrl) {
      this.router.navigateByUrl(n.actionUrl);
    }
  }

  onMarkAllRead(): void {
    this.notificationService.markAllRead().subscribe();
  }

  viewAll(): void {
    this.router.navigate(['/notifications']);
  }
}
