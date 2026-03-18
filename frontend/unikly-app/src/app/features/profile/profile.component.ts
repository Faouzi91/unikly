import { Component } from '@angular/core';

@Component({
  selector: 'app-profile',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">My Profile</h1>
      <p class="text-gray-500">Profile management will appear here.</p>
    </div>
  `,
})
export class ProfileComponent {}
