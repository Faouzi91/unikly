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
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.scss',
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
