import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

export interface ConversationSummary {
  id: string;
  jobId: string | null;
  participantIds: string[];
  createdAt: string;
  lastMessageAt: string;
}

export interface MessageItem {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  contentType: 'TEXT' | 'FILE_LINK' | 'SYSTEM';
  readAt: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly api = inject(ApiService);

  getConversations(page = 0): Observable<PageResponse<ConversationSummary>> {
    return this.api.get<PageResponse<ConversationSummary>>(
      '/v1/messages/conversations',
      { page }
    );
  }

  getMessages(
    conversationId: string,
    page = 0,
    size = 50
  ): Observable<PageResponse<MessageItem>> {
    return this.api.get<PageResponse<MessageItem>>(
      `/v1/messages/conversations/${conversationId}/messages`,
      { page, size }
    );
  }

  sendMessage(conversationId: string, content: string): Observable<MessageItem> {
    return this.api.post<MessageItem>(
      `/v1/messages/conversations/${conversationId}`,
      { content, contentType: 'TEXT' }
    );
  }

  markAsRead(messageId: string): Observable<void> {
    return this.api.patch<void>(`/v1/messages/${messageId}/read`, {});
  }

  getOrCreateConversation(
    participantIds: string[],
    jobId?: string
  ): Observable<ConversationSummary> {
    return this.api.post<ConversationSummary>('/v1/messages/conversations', {
      participantIds,
      jobId: jobId ?? null,
    });
  }
}
