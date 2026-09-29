import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '@core/cart/cart.service';
import { ProductCatalogService } from '../../data/product-catalog.service';

const products: Record<string, { name: string; price: number }> = {
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
  imports: [RouterLink],
  templateUrl: './basket-page.html',
  styleUrl: './basket-page.css',
})
export class BasketPage {
  readonly cart = inject(CartService);
  private readonly catalog = inject(ProductCatalogService);
  readonly items = computed(() =>
    this.cart.lines().map((line) => {
      const product = this.catalog.products().find((item) => item.id === line.productId);
      return {
        ...line,
        name: product?.name ?? products[line.productId]?.name ?? line.productId,
        price: product?.price ?? products[line.productId]?.price ?? 0,
      };
    }),
  );
  readonly subtotal = computed(() =>
    this.items().reduce((sum, item) => sum + item.price * item.quantity, 0),
  );

  remove(productId: string): void {
    this.cart.remove(productId);
  }

  clear(): void {
    this.cart.clear();
  }
}
