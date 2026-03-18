import { Component } from '@angular/core';

@Component({
  selector: 'app-conversation',
  standalone: true,
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Conversation</h1>
      <p class="text-gray-500">Chat messages will appear here.</p>
    </div>
  `,
})
export class ConversationComponent {}
