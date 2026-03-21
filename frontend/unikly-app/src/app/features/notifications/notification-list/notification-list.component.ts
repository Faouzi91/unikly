import { Component, OnInit, inject, signal } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatDividerModule } from '@angular/material/divider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import {
  NotificationService,
  NotificationItem,
  NotificationPreferences,
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
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    NgClass,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatPaginatorModule,
    MatDividerModule,
    MatSlideToggleModule,
    MatCardModule,
    MatProgressSpinnerModule,
    TimeAgoPipe,
  ],
  templateUrl: './notification-list.component.html',
  styleUrl: './notification-list.component.scss',
})
export class NotificationListComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly unreadCount = this.notificationService.unreadCount;

  filter: 'all' | 'unread' = 'all';
  readonly pageSize = 20;

  readonly items = signal<NotificationItem[]>([]);
  readonly totalItems = signal<number>(0);
  readonly pageIndex = signal<number>(0);
  readonly loading = signal<boolean>(false);
  readonly prefs = signal<NotificationPreferences | null>(null);

  ngOnInit(): void {
    this.notificationService.init();
    this.loadPage(0);
    this.loadPreferences();
  }

  private loadPage(page: number): void {
    this.loading.set(true);
    this.notificationService
      .getNotifications(page, this.pageSize, this.filter === 'unread')
      .subscribe({
        next: (data) => {
          this.items.set(data.content);
          this.totalItems.set(data.totalElements);
          this.pageIndex.set(data.number);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  private loadPreferences(): void {
    this.notificationService.getPreferences().subscribe({
      next: (p) => this.prefs.set(p),
    });
  }

  onFilterChange(): void {
    this.loadPage(0);
  }

  onPageChange(event: PageEvent): void {
    this.loadPage(event.pageIndex);
  }

  onItemClick(n: NotificationItem): void {
    if (!n.read) {
      this.notificationService.markAsRead(n.id).subscribe(() => {
        this.items.update((list) =>
          list.map((item) => (item.id === n.id ? { ...item, read: true } : item)),
        );
      });
    }
    if (n.actionUrl) {
      this.router.navigateByUrl(n.actionUrl);
    }
  }

  markAllRead(): void {
    this.notificationService.markAllRead().subscribe(() => {
      this.items.update((list) => list.map((n) => ({ ...n, read: true })));
    });
  }

  savePrefs(): void {
    const p = this.prefs();
    if (!p) return;
    this.notificationService.updatePreferences(p).subscribe();
  }

  iconFor(type: string): string {
    return TYPE_ICONS[type] ?? 'notifications';
  }
}
