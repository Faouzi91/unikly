import { Component } from '@angular/core';

@Component({
  selector: 'app-payments',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Payments</h1>
      <p class="text-gray-500">Payment history and escrow will appear here.</p>
    </div>
  `,
})
export class PaymentsComponent {}
