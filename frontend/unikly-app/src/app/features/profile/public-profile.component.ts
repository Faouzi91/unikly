import { Component } from '@angular/core';

@Component({
  selector: 'app-public-profile',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">User Profile</h1>
      <p class="text-gray-500">Public profile will appear here.</p>
    </div>
  `,
})
export class PublicProfileComponent {}
