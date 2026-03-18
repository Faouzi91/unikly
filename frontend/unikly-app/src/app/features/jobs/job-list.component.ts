import { Component } from '@angular/core';

@Component({
  selector: 'app-job-list',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Jobs</h1>
      <p class="text-gray-500">Job listings will appear here.</p>
    </div>
  `,
})
export class JobListComponent {}
