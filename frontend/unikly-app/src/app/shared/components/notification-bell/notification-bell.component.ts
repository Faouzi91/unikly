import { Component, ElementRef, HostListener, computed, effect, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { TimeAgoPipe } from '../../pipes/time-ago.pipe';
import { NotificationItem, NotificationService } from '../../../core/services/notification.service';

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
  selector: 'app-notification-bell',
  standalone: true,
  imports: [NgClass, TimeAgoPipe],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.scss',
})
export class NotificationBellComponent {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly unreadCount = this.notificationService.unreadCount;
  readonly recent = computed(() => this.notificationService.notifications().slice(0, 8));
  readonly open = signal(false);
  readonly pulse = signal(false);

  private previousUnread = 0;

  constructor() {
    this.notificationService.init();

    effect(() => {
      const current = this.unreadCount();
      if (current > this.previousUnread) {
        this.triggerPulse();
      }
      this.previousUnread = current;
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node | null;
    if (target && !this.host.nativeElement.contains(target)) {
      this.open.set(false);
    }
  }

  toggle(): void {
    this.open.set(!this.open());
  }

  labelFor(type: string): string {
    return TYPE_LABELS[type] ?? 'Alert';
  }

  onItemClick(item: NotificationItem): void {
    if (!item.read) {
      this.notificationService.markAsRead(item.id).subscribe();
    }
    this.open.set(false);
    if (item.actionUrl) {
      this.router.navigateByUrl(item.actionUrl);
    }
  }

  onMarkAllRead(): void {
    this.notificationService.markAllRead().subscribe();
  }

  viewAll(): void {
    this.open.set(false);
    this.router.navigate(['/notifications']);
  }

  private triggerPulse(): void {
    this.pulse.set(true);
    setTimeout(() => this.pulse.set(false), 600);
  }
}
