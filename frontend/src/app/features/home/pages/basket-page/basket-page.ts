import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '@core/cart/cart.service';
import { ProductCatalogService } from '../../data/product-catalog.service';

const fallbackProducts: Record<string, { name: string; price: number; image?: string }> = {
  'linen-throw': { name: 'Textured cotton throw blanket', price: 34.95 },
  'table-lamp': { name: 'Minimal ceramic bedside lamp', price: 48 },
  headphones: { name: 'Wireless over-ear headphones', price: 89.99 },
  'coffee-set': { name: 'Stoneware coffee cup set', price: 26.5 },
  daypack: { name: 'Everyday canvas daypack', price: 42 },
  'serving-board': { name: 'Acacia wood serving board', price: 31.75 },
  'desk-organizer': { name: 'Wood and steel desk organizer', price: 22.99 },
  'water-bottle': { name: 'Insulated stainless bottle', price: 19.95 },
};

@Component({
  selector: 'app-basket-page',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './basket-page.html',
  styleUrl: './basket-page.css',
})
export class BasketPage {
  readonly cart = inject(CartService);
  private readonly catalog = inject(ProductCatalogService);

  readonly items = computed(() =>
    this.cart.lines().map((line) => {
      const product = this.catalog.products().find((item) => item.id === line.productId);
      const price = product?.price ?? fallbackProducts[line.productId]?.price ?? 0;
      const stockQuantity = product?.stockQuantity ?? 10;
      const image = product?.image ?? fallbackProducts[line.productId]?.image ?? '/product-placeholder.svg';
      return {
        ...line,
        name: product?.name ?? fallbackProducts[line.productId]?.name ?? line.productId,
        price,
        image,
        category: product?.category ?? 'Home',
        stockQuantity,
        lineTotal: price * line.quantity,
        isAtLimit: line.quantity >= stockQuantity,
        isLowStock: stockQuantity <= 5 && stockQuantity > 0,
      };
    }),
  );

  readonly subtotal = computed(() =>
    this.items().reduce((sum, item) => sum + item.lineTotal, 0),
  );

  readonly freeDeliveryThreshold = 50;
  readonly freeDeliveryQualified = computed(() => this.subtotal() >= this.freeDeliveryThreshold);
  readonly amountNeededForFreeDelivery = computed(() =>
    Math.max(0, this.freeDeliveryThreshold - this.subtotal()),
  );
  readonly deliveryProgressPercent = computed(() =>
    Math.min(100, Math.round((this.subtotal() / this.freeDeliveryThreshold) * 100)),
  );
  readonly estimatedDeliveryFee = computed(() =>
    this.freeDeliveryQualified() ? 0 : 5,
  );
  readonly estimatedTotal = computed(() => this.subtotal() + this.estimatedDeliveryFee());

  increment(item: { productId: string; quantity: number; stockQuantity: number }): void {
    if (item.quantity < item.stockQuantity) {
      this.cart.updateQuantity(item.productId, item.quantity + 1, item.stockQuantity);
    }
  }

  decrement(item: { productId: string; quantity: number; stockQuantity: number }): void {
    if (item.quantity > 1) {
      this.cart.updateQuantity(item.productId, item.quantity - 1, item.stockQuantity);
    } else {
      this.cart.remove(item.productId);
    }
  }

  remove(productId: string): void {
    this.cart.remove(productId);
  }

  clear(): void {
    this.cart.clear();
  }
}
