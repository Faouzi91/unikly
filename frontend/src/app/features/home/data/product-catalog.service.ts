import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '@core/identity/auth.service';
import { SAMPLE_PRODUCTS, StoreProduct } from './sample-products';

export interface NewProductDetails {
  name: string;
  description: string;
  category: string;
  price: number;
  stockQuantity: number;
  image?: string;
}

@Injectable({ providedIn: 'root' })
export class ProductCatalogService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly storageKey = 'unikly-seller-products-v1';
  private readonly productsState = signal<StoreProduct[]>([...SAMPLE_PRODUCTS]);
  readonly products = this.productsState.asReadonly();

  constructor() {
    effect(() => {
      this.auth.user();
      void this.refresh();
    });
  }

  async refresh(): Promise<void> {
    try {
      const remote = await firstValueFrom(this.http.get<StoreProduct[]>('/api/products'));
      const remoteById = new Map(remote.map((product) => [product.id, product]));
      const mergedSamples = SAMPLE_PRODUCTS.map((sample) => {
        const persisted = remoteById.get(sample.id);
        if (!persisted) return sample;
        remoteById.delete(sample.id);
        const rating = (persisted.rating && persisted.rating > 0) ? persisted.rating : sample.rating;
        const reviews = (persisted.reviews && persisted.reviews > 0) ? persisted.reviews : sample.reviews;
        return { ...sample, ...persisted, rating, reviews, badge: sample.badge };
      });
      this.productsState.set([...mergedSamples, ...remoteById.values()]);
      await this.migrateCurrentSellersLocalProducts();
    } catch {
      // Keep sample products available when the API is offline.
    }
  }

  private async migrateCurrentSellersLocalProducts(): Promise<void> {
    const sellerId = this.auth.user()?.role === 'SELLER' ? this.auth.user()?.id : undefined;
    if (sellerId === undefined) return;
    let legacy: unknown;
    try {
      legacy = JSON.parse(localStorage.getItem(this.storageKey) ?? '[]');
    } catch {
      return;
    }
    if (!Array.isArray(legacy)) return;

    const mine = legacy.filter((item) => item?.sellerId === sellerId);
    let remaining = [...legacy];
    for (const item of mine) {
      if (typeof item.name !== 'string' || typeof item.description !== 'string' ||
          typeof item.category !== 'string' || typeof item.price !== 'number') continue;
      try {
        const migrated = await this.addProduct({
          name: item.name,
          description: item.description,
          category: item.category,
          price: item.price,
          stockQuantity: Number.isInteger(item.stockQuantity) && item.stockQuantity >= 0 ? item.stockQuantity : 10,
          image: typeof item.image === 'string' ? item.image : '',
        }, sellerId);
        this.productsState.update((products) => products.filter((product) => product.id !== item.id).concat(migrated));
        remaining = remaining.filter((product) => product.id !== item.id);
        localStorage.setItem(this.storageKey, JSON.stringify(remaining));
      } catch {
        // Leave un-migrated local entries intact if the API rejects them.
      }
    }
  }

  async addProduct(details: NewProductDetails, _sellerId: number): Promise<StoreProduct> {
    const product = await firstValueFrom(this.http.post<StoreProduct>('/api/products', details));
    this.productsState.update((products) => [...products, product]);
    return product;
  }

  async updateSellerProduct(productId: string, _sellerId: number, details: NewProductDetails): Promise<boolean> {
    const updated = await firstValueFrom(
      this.http.put<StoreProduct>(`/api/products/${encodeURIComponent(productId)}`, details),
    );
    this.productsState.update((products) => products.map((item) => item.id === productId ? updated : item));
    return true;
  }

  async removeSellerProduct(productId: string, _sellerId: number): Promise<boolean> {
    await firstValueFrom(this.http.delete<void>(`/api/products/${encodeURIComponent(productId)}`));
    this.productsState.update((products) => products.filter((item) => item.id !== productId));
    return true;
  }
}
