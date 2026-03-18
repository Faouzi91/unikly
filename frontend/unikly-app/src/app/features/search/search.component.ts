import { Component } from '@angular/core';

@Component({
  selector: 'app-search',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Search</h1>
      <p class="text-gray-500">Search for jobs and freelancers.</p>
    </div>
  `,
})
export class SearchComponent {}
