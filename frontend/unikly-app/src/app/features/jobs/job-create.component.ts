import { Component } from '@angular/core';

@Component({
  selector: 'app-job-create',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Post a Job</h1>
      <p class="text-gray-500">Job creation form will appear here.</p>
    </div>
  `,
})
export class JobCreateComponent {}
