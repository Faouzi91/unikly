import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationBellComponent } from './notification-bell.component';
import { NotificationService } from '../../../core/services/notification.service';
import { signal, computed } from '@angular/core';

describe('NotificationBellComponent', () => {
  let component: NotificationBellComponent;
  let fixture: ComponentFixture<NotificationBellComponent>;

  const mockNotificationService = {
    notifications: signal([]),
    unreadCount: signal(0),
    init: () => {},
    markAsRead: () => ({ subscribe: () => {} }),
    markAllRead: () => ({ subscribe: () => {} }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationBellComponent, RouterTestingModule],
      providers: [
        { provide: NotificationService, useValue: mockNotificationService },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(NotificationBellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
