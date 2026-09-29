import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
  private readonly productId = signal<string | null>(null);
  readonly product = computed(() => this.catalog.products().find((item) => item.id === this.productId()));
  readonly addedToBasket = signal(false);

  constructor(route: ActivatedRoute) {
    route.paramMap.subscribe((params) => {
      this.productId.set(params.get('id'));
      this.addedToBasket.set(false);
    });
  }

  isAtStockLimit(product: StoreProduct): boolean {
    return (this.cart.lines().find((line) => line.productId === product.id)?.quantity ?? 0) >= product.stockQuantity;
  }

  addToBasket(): void {
    const product = this.product();
    if (!product) return;
    this.addedToBasket.set(this.cart.add(product.id, product.stockQuantity));
  }

  stars(rating: number): string {
    return `${'★'.repeat(Math.floor(rating))}${'☆'.repeat(5 - Math.floor(rating))}`;
  }
}
