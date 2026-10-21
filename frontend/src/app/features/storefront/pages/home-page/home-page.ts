import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { CartService } from '@core/cart/cart.service';
import {
  LucideArrowRight,
  LucideCheck,
  LucideMinus,
  LucidePackage,
  LucidePlus,
  LucideSlidersHorizontal,
  LucideSparkles,
  LucideStar,
  LucideTrash,
} from '@lucide/angular';
import { StoreProduct } from '@core/catalog/sample-products';
import { ProductCatalogService } from '@core/catalog/product-catalog.service';

type ProductSortOrder = 'featured' | 'price-asc' | 'price-desc' | 'rating-desc';

export interface DepartmentShowcase {
  category: string;
  title: string;
  subtitle: string;
  kicker: string;
  count: number;
  inStockCount: number;
  startingPrice: number;
  primaryImage: string;
  secondaryImage: string;
}

const DEPARTMENT_META: ReadonlyArray<{
  category: string;
  title: string;
  subtitle: string;
  kicker: string;
  fallbackImage: string;
}> = [
  {
    category: 'Home',
    title: 'Home & Living',
    subtitle: 'Textured throws, warm ambient lighting, and calm workspace pieces.',
    kicker: 'Living spaces',
    fallbackImage: 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=720&q=85',
  },
  {
    category: 'Electronics',
    title: 'Electronics & Audio',
    subtitle: 'Focused wireless listening and everyday gear built for comfort.',
    kicker: 'Daily tech',
    fallbackImage: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=720&q=85',
  },
  {
    category: 'Kitchen',
    title: 'Kitchen & Dining',
    subtitle: 'Tactile stoneware cups and durable natural wood prep boards.',
    kicker: 'Table & prep',
    fallbackImage: 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=720&q=85',
  },
  {
    category: 'Outdoor',
    title: 'Outdoor & Carry',
    subtitle: 'Dependable canvas daypacks and insulated stainless hydration.',
    kicker: 'On the move',
    fallbackImage: 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=720&q=85',
  },
];

@Component({
  selector: 'app-home-page',
  imports: [
    RouterLink,
    LucideArrowRight,
    LucideCheck,
    LucideMinus,
    LucidePackage,
    LucidePlus,
    LucideSlidersHorizontal,
    LucideSparkles,
    LucideStar,
    LucideTrash,
  ],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  readonly cart = inject(CartService);
  private readonly catalog = inject(ProductCatalogService);
  readonly products = this.catalog.products;
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);

  readonly query = signal('');
  readonly activeCategory = signal('All');
  readonly inStockOnly = signal(false);
  readonly under35Only = signal(false);
  readonly sortOrder = signal<ProductSortOrder>('featured');
  readonly notice = signal('');
  readonly categories = ['All', 'Home', 'Electronics', 'Kitchen', 'Outdoor'];

  readonly inStockCount = computed(
    () => this.products().filter((product) => product.stockQuantity > 0).length,
  );

  readonly under35Products = computed(() =>
    this.products().filter((product) => product.price <= 35 && product.stockQuantity > 0),
  );

  readonly featuredSpotlight = computed<StoreProduct | null>(() => {
    const items = this.products();
    return (
      items.find((product) => product.stockQuantity > 0 && product.category === 'Home') ??
      items.find((product) => product.stockQuantity > 0) ??
      items[0] ??
      null
    );
  });

  readonly departmentCards = computed<DepartmentShowcase[]>(() => {
    const allProducts = this.products();
    return DEPARTMENT_META.map((meta) => {
      const matching = allProducts.filter((product) => product.category === meta.category);
      const inStock = matching.filter((product) => product.stockQuantity > 0);
      const lowestPrice =
        matching.length > 0 ? Math.min(...matching.map((product) => product.price)) : 0;
      return {
        category: meta.category,
        title: meta.title,
        subtitle: meta.subtitle,
        kicker: meta.kicker,
        count: matching.length,
        inStockCount: inStock.length,
        startingPrice: lowestPrice,
        primaryImage: matching[0]?.image ?? meta.fallbackImage,
        secondaryImage: matching[1]?.image ?? matching[0]?.image ?? meta.fallbackImage,
      };
    });
  });

  readonly hasActiveFilters = computed(
    () =>
      this.activeCategory() !== 'All' ||
      this.query().trim().length > 0 ||
      this.inStockOnly() ||
      this.under35Only(),
  );

  readonly resultCount = computed(() => this.filteredProducts().length);

  readonly filteredProducts = computed(() => {
    const search = this.query().trim().toLocaleLowerCase();
    return this.products().filter(
      (product) =>
        (this.activeCategory() === 'All' || product.category === this.activeCategory()) &&
        (!this.inStockOnly() || product.stockQuantity > 0) &&
        (!this.under35Only() || product.price <= 35) &&
        (!search ||
          `${product.name} ${product.category} ${product.description}`
            .toLocaleLowerCase()
            .includes(search)),
    );
  });

  readonly sortedProducts = computed(() => {
    const products = [...this.filteredProducts()];
    switch (this.sortOrder()) {
      case 'price-asc':
        return products.sort((a, b) => a.price - b.price);
      case 'price-desc':
        return products.sort((a, b) => b.price - a.price);
      case 'rating-desc':
        return products.sort((a, b) => b.rating - a.rating || b.reviews - a.reviews);
      default:
        return products;
    }
  });

  constructor() {
    this.title.setTitle('Unikly | Browse products');
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.query.set(params.get('q') ?? '');
      const categoryParam = params.get('category');
      if (categoryParam && this.categories.includes(categoryParam)) {
        this.activeCategory.set(categoryParam);
      } else if (categoryParam === null && !params.has('q')) {
        this.activeCategory.set('All');
      }
    });
  }

  categoryCount(category: string): number {
    return category === 'All'
      ? this.products().length
      : this.products().filter((product) => product.category === category).length;
  }

  selectCategory(category: string): void {
    this.activeCategory.set(category);
  }

  toggleUnder35(): void {
    this.under35Only.update((value) => !value);
  }

  toggleInStockOnly(): void {
    this.inStockOnly.update((value) => !value);
  }

  clearFilters(): void {
    this.activeCategory.set('All');
    this.query.set('');
    this.inStockOnly.set(false);
    this.under35Only.set(false);
    this.sortOrder.set('featured');
  }

  wholeDollars(price: number): string {
    return Math.floor(price).toString();
  }

  cents(price: number): string {
    return price.toFixed(2).split('.')[1] ?? '00';
  }

  getQuantityInCart(productId: string): number {
    return this.cart.lines().find((line) => line.productId === productId)?.quantity ?? 0;
  }

  canAdd(product: StoreProduct): boolean {
    const quantityInCart = this.getQuantityInCart(product.id);
    return product.stockQuantity > quantityInCart;
  }

  addToCart(product: StoreProduct): void {
    if (this.cart.add(product.id, product.stockQuantity)) {
      this.notice.set(`${product.name} added to your basket.`);
    } else {
      this.notice.set(
        product.stockQuantity === 0
          ? `${product.name} is out of stock.`
          : `You already have all ${product.stockQuantity} available in your basket.`,
      );
    }
  }

  incrementInCart(product: StoreProduct): void {
    const current = this.getQuantityInCart(product.id);
    if (current < product.stockQuantity) {
      this.cart.updateQuantity(product.id, current + 1, product.stockQuantity);
      this.notice.set(`${product.name} quantity increased to ${current + 1}.`);
    } else {
      this.notice.set(`All ${product.stockQuantity} available units are in your basket.`);
    }
  }

  decrementInCart(product: StoreProduct): void {
    const current = this.getQuantityInCart(product.id);
    if (current > 1) {
      this.cart.updateQuantity(product.id, current - 1, product.stockQuantity);
      this.notice.set(`${product.name} quantity updated to ${current - 1}.`);
    } else if (current === 1) {
      this.cart.remove(product.id);
      this.notice.set(`${product.name} removed from your basket.`);
    }
  }
}
