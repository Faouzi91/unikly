import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { CartService } from '@core/cart/cart.service';
import { StoreProduct } from '../../data/sample-products';
import { ProductCatalogService } from '../../data/product-catalog.service';

@Component({
  selector: 'app-product-detail-page',
  imports: [RouterLink],
  templateUrl: './product-detail-page.html',
  styleUrl: './product-detail-page.css',
})
export class ProductDetailPage {
  readonly cart = inject(CartService);
  private readonly catalog = inject(ProductCatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly paramId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id'))),
    { initialValue: null },
  );
  readonly product = computed(() => this.catalog.products().find((item) => item.id === this.paramId()));
  readonly addedToBasket = signal(false);

  readonly quantityInBasket = computed(() => {
    const p = this.product();
    if (!p) return 0;
    return this.cart.lines().find((line) => line.productId === p.id)?.quantity ?? 0;
  });

  isAtStockLimit(product: StoreProduct): boolean {
    return this.quantityInBasket() >= product.stockQuantity;
  }

  addToBasket(): void {
    const product = this.product();
    if (!product) return;
    const added = this.cart.add(product.id, product.stockQuantity);
    this.addedToBasket.set(added);
  }

  increment(): void {
    const p = this.product();
    if (!p) return;
    const current = this.quantityInBasket();
    if (current < p.stockQuantity) {
      if (current === 0) {
        this.cart.add(p.id, p.stockQuantity);
      } else {
        this.cart.updateQuantity(p.id, current + 1, p.stockQuantity);
      }
      this.addedToBasket.set(true);
    }
  }

  decrement(): void {
    const p = this.product();
    if (!p) return;
    const current = this.quantityInBasket();
    if (current > 1) {
      this.cart.updateQuantity(p.id, current - 1, p.stockQuantity);
    } else if (current === 1) {
      this.cart.remove(p.id);
      this.addedToBasket.set(false);
    }
  }

  stars(rating: number): string {
    return `${'★'.repeat(Math.floor(rating))}${'☆'.repeat(5 - Math.floor(rating))}`;
  }
}
