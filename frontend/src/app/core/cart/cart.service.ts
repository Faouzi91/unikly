import { computed, Injectable, signal } from '@angular/core';

export interface CartLine {
  productId: string;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly storageKey = 'unikly-demo-cart-v1';
  private readonly linesState = signal<CartLine[]>(this.readLines());
  readonly lines = this.linesState.asReadonly();
  readonly itemCount = computed(() =>
    this.linesState().reduce((total, line) => total + line.quantity, 0),
  );

  add(productId: string, availableQuantity = Number.POSITIVE_INFINITY): boolean {
    if (availableQuantity < 1) return false;
    const lines = [...this.linesState()];
    const existing = lines.find((line) => line.productId === productId);
    if (existing) {
      if (existing.quantity >= availableQuantity) return false;
      existing.quantity += 1;
    } else lines.push({ productId, quantity: 1 });
    this.write(lines);
    return true;
  }

  remove(productId: string): void {
    this.write(this.linesState().filter((line) => line.productId !== productId));
  }

  clear(): void {
    this.write([]);
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

  private write(lines: CartLine[]): void {
    this.linesState.set(lines);
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(lines));
    } catch {
      // Keep the in-memory cart usable if browser storage is unavailable.
    }
  }
}
