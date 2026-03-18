import { Component } from '@angular/core';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Job Details</h1>
      <p class="text-gray-500">Job details will appear here.</p>
    </div>
  `,
})
export class JobDetailComponent {}
