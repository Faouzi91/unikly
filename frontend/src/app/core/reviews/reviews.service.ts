import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ReviewItem {
  id: number;
  productId: string;
  buyerId: number;
  authorName: string;
  rating: number;
  title: string;
  comment: string;
  isVerifiedPurchase: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductReviewSummary {
  productId: string;
  averageRating: number;
  totalReviews: number;
  ratingCounts: Record<string, number>;
  reviews: ReviewItem[];
  currentUserCanReview: boolean;
  currentUserVerifiedBuyer: boolean;
  currentUserReview: ReviewItem | null;
}

export interface SubmitReviewRequest {
  rating: number;
  title: string;
  comment: string;
}

@Injectable({ providedIn: 'root' })
export class ReviewsService {
  private readonly http = inject(HttpClient);

  getProductReviews(productId: string): Observable<ProductReviewSummary> {
    return this.http.get<ProductReviewSummary>(`/api/products/${encodeURIComponent(productId)}/reviews`);
  }

  submitReview(productId: string, data: SubmitReviewRequest): Observable<ReviewItem> {
    return this.http.post<ReviewItem>(`/api/products/${encodeURIComponent(productId)}/reviews`, data);
  }

  deleteMyReview(productId: string): Observable<void> {
    return this.http.delete<void>(`/api/products/${encodeURIComponent(productId)}/reviews/mine`);
  }
}
