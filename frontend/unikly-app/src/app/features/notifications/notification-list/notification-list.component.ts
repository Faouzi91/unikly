import { Component, OnInit, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import {
  NotificationItem,
  NotificationPreferences,
  NotificationService,
} from '../../../core/services/notification.service';

const TYPE_LABELS: Record<string, string> = {
  JOB_MATCHED: 'Job',
  PROPOSAL_RECEIVED: 'Proposal',
  PROPOSAL_ACCEPTED: 'Accepted',
  PAYMENT_FUNDED: 'Escrow',
  ESCROW_RELEASED: 'Released',
  MESSAGE_RECEIVED: 'Message',
  SYSTEM: 'System',
};

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [NgClass, FormsModule, TimeAgoPipe],
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
  readonly totalItems = signal(0);
  readonly pageIndex = signal(0);
  readonly loading = signal(false);
  readonly prefs = signal<NotificationPreferences | null>(null);

  ngOnInit(): void {
    this.notificationService.init();
    this.loadPage(0);
    this.loadPreferences();
  }

  labelFor(type: string): string {
    return TYPE_LABELS[type] ?? 'Alert';
  }

  onFilterChange(nextFilter: 'all' | 'unread'): void {
    if (this.filter === nextFilter) return;
    this.filter = nextFilter;
    this.loadPage(0);
  }

  previousPage(): void {
    if (this.pageIndex() === 0) return;
    this.loadPage(this.pageIndex() - 1);
  }

  nextPage(): void {
    const next = this.pageIndex() + 1;
    if (next >= this.totalPages()) return;
    this.loadPage(next);
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems() / this.pageSize));
  }

  onItemClick(item: NotificationItem): void {
    if (!item.read) {
      this.notificationService.markAsRead(item.id).subscribe(() => {
        this.items.update((current) =>
          current.map((value) => (value.id === item.id ? { ...value, read: true } : value)),
        );
      });
    }

    if (item.actionUrl) {
      this.router.navigateByUrl(item.actionUrl);
    }
  }

  markAllRead(): void {
    this.notificationService.markAllRead().subscribe(() => {
      this.items.update((current) => current.map((item) => ({ ...item, read: true })));
    });
  }

  updatePref<K extends keyof NotificationPreferences>(key: K, value: NotificationPreferences[K]): void {
    const current = this.prefs();
    if (!current) return;
    const next = { ...current, [key]: value };
    this.prefs.set(next);
    this.notificationService.updatePreferences(next).subscribe();
  }

  private loadPage(page: number): void {
    this.loading.set(true);
    this.notificationService
      .getNotifications(page, this.pageSize, this.filter === 'unread')
      .subscribe({
        next: (response) => {
          this.items.set(response.content);
          this.totalItems.set(response.totalElements);
          this.pageIndex.set(response.number);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  private loadPreferences(): void {
    this.notificationService.getPreferences().subscribe({
      next: (prefs) => this.prefs.set(prefs),
    });
  }
}
