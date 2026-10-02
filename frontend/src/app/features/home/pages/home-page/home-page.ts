import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CartService } from '@core/cart/cart.service';
import { StoreProduct } from '../../data/sample-products';
import { ProductCatalogService } from '../../data/product-catalog.service';

type ProductSortOrder = 'featured' | 'price-asc' | 'price-desc';


@Component({
  selector: 'app-home-page',
  imports: [RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  readonly cart = inject(CartService);
  private readonly catalog = inject(ProductCatalogService);
  readonly products = this.catalog.products;
  private readonly route = inject(ActivatedRoute);
  readonly query = signal('');
  readonly activeCategory = signal('All');
  readonly sortOrder = signal<ProductSortOrder>('featured');
  readonly notice = signal('');
  readonly categories = ['All', 'Home', 'Electronics', 'Kitchen', 'Outdoor'];
  readonly filteredProducts = computed(() => {
    const search = this.query().trim().toLocaleLowerCase();
    return this.products().filter(
      (product) =>
        (this.activeCategory() === 'All' || product.category === this.activeCategory()) &&
        (!search || `${product.name} ${product.category}`.toLocaleLowerCase().includes(search)),
    );
  });

  readonly sortedProducts = computed(() => {
    const products = [...this.filteredProducts()];
    switch (this.sortOrder()) {
      case 'price-asc':
        return products.sort((a, b) => a.price - b.price);
      case 'price-desc':
        return products.sort((a, b) => b.price - a.price);
      default:
        return products;
    }
  });

  constructor() {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed())
      .subscribe((params) => this.query.set(params.get('q') ?? ''));
  }

  canAdd(product: StoreProduct): boolean {
    const quantityInCart = this.cart.lines().find((line) => line.productId === product.id)?.quantity ?? 0;
    return product.stockQuantity > quantityInCart;
  }

  addToCart(product: StoreProduct): void {
    if (this.cart.add(product.id, product.stockQuantity)) {
      this.notice.set(`${product.name} added to your basket.`);
    } else {
      this.notice.set(product.stockQuantity === 0
        ? `${product.name} is out of stock.`
        : `You already have all ${product.stockQuantity} available in your basket.`);
    }
  }

  stars(rating: number): string {
    return `${'★'.repeat(Math.floor(rating))}${'☆'.repeat(5 - Math.floor(rating))}`;
  }
}
