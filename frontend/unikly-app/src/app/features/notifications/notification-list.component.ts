import { Component } from '@angular/core';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Notifications</h1>
      <p class="text-gray-500">Your notifications will appear here.</p>
    </div>
  `,
})
export class NotificationListComponent {}
