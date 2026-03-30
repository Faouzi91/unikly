import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly activeRequests = signal(0);
  private delayTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly showSpinner = signal(false);

  /** True when there are pending requests AND the 500ms delay has elapsed. */
  readonly isLoading = computed(() => this.showSpinner() && this.activeRequests() > 0);

  start(): void {
    this.activeRequests.update((n) => n + 1);
    if (!this.delayTimer && !this.showSpinner()) {
      this.delayTimer = setTimeout(() => {
        if (this.activeRequests() > 0) {
          this.showSpinner.set(true);
        }
        this.delayTimer = null;
      }, 500);
    }
  }

  stop(): void {
    this.activeRequests.update((n) => Math.max(0, n - 1));
    if (this.activeRequests() === 0) {
      this.showSpinner.set(false);
      if (this.delayTimer) {
        clearTimeout(this.delayTimer);
        this.delayTimer = null;
      }
    }
  }
}
