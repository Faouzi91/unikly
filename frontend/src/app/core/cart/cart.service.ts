import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '@core/identity/auth.service';

export interface CartLine {
  productId: string;
  quantity: number;
}

export interface ServerCartItemView {
  productId: string;
  productName: string;
  unitPrice: number;
  image: string;
  quantity: number;
  stockQuantity: number;
  lineTotal: number;
}

export interface ServerCartView {
  id: string | null;
  buyerId: number | null;
  items: ServerCartItemView[];
  subtotal: number;
  itemCount: number;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly storageKey = 'unikly-demo-cart-v1';

  private readonly linesState = signal<CartLine[]>(this.readLines());
  readonly lines = this.linesState.asReadonly();
  readonly itemCount = computed(() =>
    this.linesState().reduce((total, line) => total + line.quantity, 0),
  );

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user?.role === 'BUYER') {
        void this.syncBuyerCart();
      } else {
        this.linesState.set(this.readLines());
      }
    });
  }

  add(productId: string, availableQuantity = Number.POSITIVE_INFINITY): boolean {
    if (availableQuantity < 1) return false;
    const lines = [...this.linesState()];
    const existing = lines.find((line) => line.productId === productId);
    if (existing) {
      if (existing.quantity >= availableQuantity) return false;
      existing.quantity += 1;
    } else {
      lines.push({ productId, quantity: 1 });
    }

    this.linesState.set(lines);

    if (this.auth.user()?.role === 'BUYER') {
      void this.addItemOnServer(productId, 1);
    } else {
      this.writeLocal(lines);
    }
    return true;
  }

  remove(productId: string): void {
    const updated = this.linesState().filter((line) => line.productId !== productId);
    this.linesState.set(updated);

    if (this.auth.user()?.role === 'BUYER') {
      void this.removeItemOnServer(productId);
    } else {
      this.writeLocal(updated);
    }
  }

  clear(): void {
    this.linesState.set([]);
    this.writeLocal([]);

    if (this.auth.user()?.role === 'BUYER') {
      void this.clearOnServer();
    }
  }

  private async syncBuyerCart(): Promise<void> {
    const localLines = this.readLines();
    try {
      if (localLines.length > 0) {
        const view = await firstValueFrom(
          this.http.post<ServerCartView>('/api/cart/merge', {
            items: localLines.map((l) => ({ productId: l.productId, quantity: l.quantity })),
          }),
        );
        this.writeLocal([]);
        this.setLinesFromView(view);
      } else {
        const view = await firstValueFrom(this.http.get<ServerCartView>('/api/cart'));
        this.setLinesFromView(view);
      }
    } catch {
      // Keep optimistic local lines on connection error
    }
  }

  private async addItemOnServer(productId: string, quantity: number): Promise<void> {
    try {
      const view = await firstValueFrom(
        this.http.post<ServerCartView>('/api/cart/items', { productId, quantity }),
      );
      this.setLinesFromView(view);
    } catch {
      void this.fetchServerCart();
    }
  }

  private async removeItemOnServer(productId: string): Promise<void> {
    try {
      const view = await firstValueFrom(
        this.http.delete<ServerCartView>(`/api/cart/items/${encodeURIComponent(productId)}`),
      );
      this.setLinesFromView(view);
    } catch {
      void this.fetchServerCart();
    }
  }

  private async clearOnServer(): Promise<void> {
    try {
      await firstValueFrom(this.http.delete('/api/cart'));
    } catch {
      // Best effort
    }
  }

  private async fetchServerCart(): Promise<void> {
    try {
      const view = await firstValueFrom(this.http.get<ServerCartView>('/api/cart'));
      this.setLinesFromView(view);
    } catch {
      // Keep current state
    }
  }

  private setLinesFromView(view: ServerCartView): void {
    if (!view || !Array.isArray(view.items)) return;
    const lines = view.items.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
    }));
    this.linesState.set(lines);
  }

  private readLines(): CartLine[] {
    try {
      const value: unknown = JSON.parse(localStorage.getItem(this.storageKey) ?? '[]');
      if (!Array.isArray(value)) return [];
      return value.filter(
        (line): line is CartLine =>
          typeof line?.productId === 'string' &&
          Number.isInteger(line?.quantity) &&
          line.quantity > 0,
      );
    } catch {
      return [];
    }
  }

  private writeLocal(lines: CartLine[]): void {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(lines));
    } catch {
      // Keep the in-memory cart usable if browser storage is unavailable.
    }
  }
}
