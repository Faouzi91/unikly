import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationListComponent } from './notification-list.component';
import { NotificationService } from '../../../core/services/notification.service';
import { signal, computed } from '@angular/core';
import { of } from 'rxjs';

describe('NotificationListComponent', () => {
  let component: NotificationListComponent;
  let fixture: ComponentFixture<NotificationListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationListComponent, RouterTestingModule],
      providers: [
        {
          provide: NotificationService,
          useValue: {
            unreadCount: signal(0),
            init: () => {},
            getNotifications: () => of({ content: [], totalElements: 0, number: 0 }),
            getPreferences: () => of(null),
            markAsRead: () => of({}),
            markAllRead: () => of({}),
            updatePreferences: () => of({}),
          },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(NotificationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
