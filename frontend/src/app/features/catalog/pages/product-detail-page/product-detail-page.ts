import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { map, firstValueFrom } from 'rxjs';
import { CartService } from '@core/cart/cart.service';
import { AuthService } from '@core/identity/auth.service';
import { ReviewsService, ProductReviewSummary } from '@core/reviews/reviews.service';
import {
  LucideCheck,
  LucideCircleAlert,
  LucideMessageSquare,
  LucideMinus,
  LucidePlus,
  LucideShieldCheck,
  LucideSquarePen,
  LucideStar,
  LucideTrash,
  LucideUser,
  LucideX,
} from '@lucide/angular';
import { StoreProduct } from '@core/catalog/sample-products';
import { ProductCatalogService, SellerStoreProfile } from '@core/catalog/product-catalog.service';

export interface StarBreakdownRow {
  stars: number;
  count: number;
  percentage: number;
}

@Component({
  selector: 'app-product-detail-page',
  imports: [
    RouterLink,
    FormsModule,
    DatePipe,
    DecimalPipe,
    LucideCheck,
    LucideCircleAlert,
    LucideMessageSquare,
    LucideMinus,
    LucidePlus,
    LucideShieldCheck,
    LucideSquarePen,
    LucideStar,
    LucideTrash,
    LucideUser,
    LucideX,
  ],
  templateUrl: './product-detail-page.html',
  styleUrl: './product-detail-page.css',
})
export class ProductDetailPage {
  readonly cart = inject(CartService);
  readonly auth = inject(AuthService);
  private readonly catalog = inject(ProductCatalogService);
  private readonly reviewsService = inject(ReviewsService);
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);

  private readonly paramId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id'))),
    { initialValue: null },
  );

  readonly product = computed(() => this.catalog.products().find((item) => item.id === this.paramId()));
  readonly addedToBasket = signal(false);
  readonly sellerProfile = signal<SellerStoreProfile | null>(null);

  // Reviews state
  readonly reviewSummary = signal<ProductReviewSummary | null>(null);
  readonly isLoadingReviews = signal(false);
  readonly reviewFormOpen = signal(false);
  readonly formRating = signal(5);
  readonly formTitle = signal('');
  readonly formComment = signal('');
  readonly formSubmitting = signal(false);
  readonly formError = signal<string | null>(null);
  readonly formSuccess = signal<string | null>(null);

  readonly quantityInBasket = computed(() => {
    const p = this.product();
    if (!p) return 0;
    return this.cart.lines().find((line) => line.productId === p.id)?.quantity ?? 0;
  });

  readonly totalReviews = computed(() => {
    if (this.product()?.isDemo) return 0;
    const summary = this.reviewSummary();
    if (summary) return summary.totalReviews;
    return this.product()?.reviews ?? 0;
  });

  readonly averageRating = computed(() => {
    if (this.product()?.isDemo) return 0;
    const summary = this.reviewSummary();
    if (summary && summary.totalReviews > 0) return summary.averageRating;
    return this.product()?.rating ?? 0.0;
  });

  readonly userReview = computed(() => this.reviewSummary()?.currentUserReview ?? null);

  readonly canReview = computed(() => {
    if (this.product()?.isDemo) return false;
    return this.reviewSummary()?.currentUserCanReview ?? false;
  });

  readonly isVerifiedBuyer = computed(() => this.reviewSummary()?.currentUserVerifiedBuyer ?? false);

  readonly starBreakdown = computed<StarBreakdownRow[]>(() => {
    const summary = this.reviewSummary();
    const total = summary?.totalReviews ?? 0;
    const counts = summary?.ratingCounts ?? {};
    const rows: StarBreakdownRow[] = [];
    for (let star = 5; star >= 1; star--) {
      const count = Number(counts[String(star)] ?? counts[star as unknown as string] ?? 0);
      const percentage = total > 0 ? Math.round((count / total) * 100) : 0;
      rows.push({ stars: star, count, percentage });
    }
    return rows;
  });

  constructor() {
    effect(() => {
      const id = this.paramId();
      const currentProduct = this.product();
      this.title.setTitle(currentProduct ? `${currentProduct.name} | Unikly` : 'Product not found | Unikly');
      this.auth.user(); // reload when user logs in/out
      if (currentProduct?.isDemo) {
        this.reviewSummary.set(null);
        this.sellerProfile.set(null);
      } else {
        if (id) {
          void this.loadReviews(id);
        }
        if (currentProduct?.sellerId) {
          void this.loadSellerProfile(currentProduct.sellerId);
        } else {
          this.sellerProfile.set(null);
        }
      }
    });
  }

  async loadSellerProfile(sellerId: number): Promise<void> {
    try {
      const profile = await this.catalog.getSellerProfile(sellerId);
      this.sellerProfile.set(profile);
    } catch {
      this.sellerProfile.set(null);
    }
  }

  async loadReviews(productId: string): Promise<void> {
    this.isLoadingReviews.set(true);
    try {
      const summary = await firstValueFrom(this.reviewsService.getProductReviews(productId));
      this.reviewSummary.set(summary);
    } catch {
      // If reviews API is unavailable or returns 404
      this.reviewSummary.set(null);
    } finally {
      this.isLoadingReviews.set(false);
    }
  }

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

  openReviewForm(): void {
    const existing = this.userReview();
    if (existing) {
      this.formRating.set(existing.rating);
      this.formTitle.set(existing.title);
      this.formComment.set(existing.comment);
    } else {
      this.formRating.set(5);
      this.formTitle.set('');
      this.formComment.set('');
    }
    this.formError.set(null);
    this.formSuccess.set(null);
    this.reviewFormOpen.set(true);
  }

  closeReviewForm(): void {
    this.reviewFormOpen.set(false);
    this.formError.set(null);
  }

  setRating(stars: number): void {
    this.formRating.set(stars);
  }

  onRatingKeydown(event: KeyboardEvent, currentStar: number): void {
    let nextStar: number | null = null;
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
      nextStar = currentStar < 5 ? currentStar + 1 : 1;
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
      nextStar = currentStar > 1 ? currentStar - 1 : 5;
    } else if (event.key === 'Home') {
      nextStar = 1;
    } else if (event.key === 'End') {
      nextStar = 5;
    }
    if (nextStar !== null) {
      event.preventDefault();
      this.formRating.set(nextStar);
      const container = (event.currentTarget as HTMLElement | null)?.parentElement;
      const targetButton = container?.querySelector<HTMLButtonElement>(`button[data-star="${nextStar}"]`);
      targetButton?.focus();
    }
  }

  async submitReview(): Promise<void> {
    const p = this.product();
    if (!p) return;
    const title = this.formTitle().trim();
    const comment = this.formComment().trim();
    const rating = this.formRating();

    if (!title) {
      this.formError.set('Please provide a title for your review.');
      return;
    }
    if (!comment) {
      this.formError.set('Please write a detailed review comment.');
      return;
    }

    this.formSubmitting.set(true);
    this.formError.set(null);

    try {
      await firstValueFrom(this.reviewsService.submitReview(p.id, { rating, title, comment }));
      this.formSuccess.set('Your review has been successfully published!');
      this.reviewFormOpen.set(false);
      await this.loadReviews(p.id);
      await this.catalog.refresh();
    } catch (err: unknown) {
      const errorMsg = (err as { error?: { message?: string } })?.error?.message;
      this.formError.set(errorMsg || 'Failed to submit your review. Please try again.');
    } finally {
      this.formSubmitting.set(false);
    }
  }

  async deleteReview(): Promise<void> {
    const p = this.product();
    if (!p) return;
    if (!confirm('Are you sure you want to delete your review?')) return;

    try {
      await firstValueFrom(this.reviewsService.deleteMyReview(p.id));
      this.formSuccess.set('Your review has been removed.');
      this.reviewFormOpen.set(false);
      await this.loadReviews(p.id);
      await this.catalog.refresh();
    } catch {
      this.formError.set('Failed to delete your review.');
    }
  }

  scrollToReviews(event: Event): void {
    event.preventDefault();
    const elem = document.getElementById('customer-reviews');
    if (elem) {
      elem.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
