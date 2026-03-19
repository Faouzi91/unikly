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
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';
import {
  NotificationService,
  NotificationItem,
  NotificationPreferences,
} from '../../core/services/notification.service';

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
  template: `
    <div class="max-w-3xl mx-auto p-6">

      <!-- Page header -->
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">Notifications</h1>
        <button
          mat-stroked-button
          color="primary"
          [disabled]="unreadCount() === 0"
          (click)="markAllRead()"
        >
          <mat-icon>done_all</mat-icon>
          Mark all read
        </button>
      </div>

      <!-- Filter toggle -->
      <mat-button-toggle-group
        [(ngModel)]="filter"
        (change)="onFilterChange()"
        class="mb-4"
        aria-label="Notification filter"
      >
        <mat-button-toggle value="all">All</mat-button-toggle>
        <mat-button-toggle value="unread">
          Unread
          @if (unreadCount() > 0) {
            <span class="ml-1 bg-blue-500 text-white text-xs rounded-full px-1.5 py-0.5">
              {{ unreadCount() }}
            </span>
          }
        </mat-button-toggle>
      </mat-button-toggle-group>

      <!-- Loading spinner -->
      @if (loading()) {
        <div class="flex justify-center py-12">
          <mat-spinner diameter="40" />
        </div>
      }

      <!-- Empty state -->
      @if (!loading() && items().length === 0) {
        <div class="text-center py-12 text-gray-400">
          <mat-icon class="!text-6xl text-gray-300 block mb-3">notifications_none</mat-icon>
          <p class="text-lg">{{ filter === 'unread' ? 'No unread notifications' : 'No notifications yet' }}</p>
        </div>
      }

      <!-- Notification list -->
      @if (!loading()) {
        <div class="space-y-1">
          @for (n of items(); track n.id) {
            <div
              class="flex items-start gap-4 p-4 rounded-lg cursor-pointer transition-colors hover:bg-gray-50"
              [ngClass]="{ 'bg-blue-50 hover:bg-blue-100': !n.read }"
              (click)="onItemClick(n)"
              role="button"
              tabindex="0"
              (keydown.enter)="onItemClick(n)"
            >
              <div class="shrink-0 w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
                <mat-icon class="text-blue-600 !text-xl">{{ iconFor(n.type) }}</mat-icon>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-2">
                  <span class="font-medium text-sm" [ngClass]="{ 'font-semibold': !n.read }">
                    {{ n.title }}
                  </span>
                  <span class="text-xs text-gray-400 shrink-0">{{ n.createdAt | timeAgo }}</span>
                </div>
                <p class="text-sm text-gray-600 mt-0.5">{{ n.body }}</p>
              </div>
              @if (!n.read) {
                <span class="w-2.5 h-2.5 rounded-full bg-blue-500 shrink-0 mt-1.5"></span>
              }
            </div>
          }
        </div>

        <!-- Paginator -->
        @if (totalItems() > pageSize) {
          <mat-paginator
            [length]="totalItems()"
            [pageSize]="pageSize"
            [pageIndex]="pageIndex()"
            [pageSizeOptions]="[10, 20, 50]"
            (page)="onPageChange($event)"
            class="mt-4"
          />
        }
      }

      <!-- Preferences section -->
      <mat-divider class="my-8" />

      <mat-card>
        <mat-card-header>
          <mat-card-title class="!text-lg">Notification preferences</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (prefs()) {
            <div class="space-y-4 mt-4">
              <div class="flex items-center justify-between">
                <div>
                  <p class="font-medium text-sm">Email notifications</p>
                  <p class="text-xs text-gray-500">Receive notifications via email</p>
                </div>
                <mat-slide-toggle
                  [(ngModel)]="prefs()!.emailEnabled"
                  (change)="savePrefs()"
                />
              </div>
              <mat-divider />
              <div class="flex items-center justify-between">
                <div>
                  <p class="font-medium text-sm">Push notifications</p>
                  <p class="text-xs text-gray-500">Receive push notifications on your device</p>
                </div>
                <mat-slide-toggle
                  [(ngModel)]="prefs()!.pushEnabled"
                  (change)="savePrefs()"
                />
              </div>
              <mat-divider />
              <div class="flex items-center justify-between">
                <div>
                  <p class="font-medium text-sm">Real-time notifications</p>
                  <p class="text-xs text-gray-500">Show in-app notifications instantly</p>
                </div>
                <mat-slide-toggle
                  [(ngModel)]="prefs()!.realtimeEnabled"
                  (change)="savePrefs()"
                />
              </div>
            </div>
          } @else {
            <div class="flex justify-center py-6">
              <mat-spinner diameter="32" />
            </div>
          }
        </mat-card-content>
      </mat-card>

    </div>
  `,
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
