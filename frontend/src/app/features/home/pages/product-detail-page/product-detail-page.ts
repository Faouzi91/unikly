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
