import { Injectable, signal } from '@angular/core';

export type ToastVariant = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  message: string;
  variant: ToastVariant;
  durationMs: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<ToastMessage[]>([]);

  show(message: string, variant: ToastVariant = 'info', durationMs = 5000): void {
    const toast: ToastMessage = {
      id: crypto.randomUUID(),
      message,
      variant,
      durationMs,
    };
    this.toasts.update((items) => [...items, toast]);
    setTimeout(() => this.dismiss(toast.id), durationMs);
  }

  success(message: string, durationMs = 4000): void {
    this.show(message, 'success', durationMs);
  }

  error(message: string, durationMs = 5500): void {
    this.show(message, 'error', durationMs);
  }

  warning(message: string, durationMs = 5000): void {
    this.show(message, 'warning', durationMs);
  }

  info(message: string, durationMs = 4500): void {
    this.show(message, 'info', durationMs);
  }

  dismiss(id: string): void {
    this.toasts.update((items) => items.filter((item) => item.id !== id));
  }
}
