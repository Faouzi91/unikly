import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/identity/auth.service';
import { ProductCatalogService } from '../../data/product-catalog.service';
import { StoreProduct } from '../../data/sample-products';

@Component({
  selector: 'app-my-products-page',
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './my-products-page.html',
  styleUrl: './my-products-page.css',
})
export class MyProductsPage {
  private readonly auth = inject(AuthService);
  private readonly catalog = inject(ProductCatalogService);
  readonly categories = ['Home', 'Electronics', 'Kitchen', 'Outdoor'];
  readonly products = computed(() => {
    const sellerId = this.auth.user()?.id;
    return sellerId === undefined
      ? []
      : this.catalog.products().filter((product) => product.sellerId === sellerId);
  });
  readonly editingId = signal<string | null>(null);
  readonly pendingDeleteId = signal<string | null>(null);
  readonly notice = signal('');
  name = '';
  description = '';
  category = 'Home';
  price: number | null = null;
  stockQuantity = 10;
  image = '';

  startEdit(product: StoreProduct): void {
    this.editingId.set(product.id);
    this.pendingDeleteId.set(null);
    this.name = product.name;
    this.description = product.description;
    this.category = product.category;
    this.price = product.price;
    this.stockQuantity = product.stockQuantity;
    this.image = product.image.startsWith('http://') || product.image.startsWith('https://') ? product.image : '';
    this.notice.set('');
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.resetForm();
  }

  async saveEdit(productId: string, invalid: boolean | null): Promise<void> {
    const sellerId = this.auth.user()?.id;
    if (sellerId === undefined || this.price === null || invalid || this.price <= 0) return;

    try {
      const updated = await this.catalog.updateSellerProduct(productId, sellerId, {
        name: this.name.trim(),
        description: this.description.trim(),
        category: this.category,
        price: this.price,
        stockQuantity: this.stockQuantity,
        image: this.image.trim(),
      });
      if (!updated) return;

      this.editingId.set(null);
      this.resetForm();
      this.notice.set('Your product listing was updated.');
    } catch {
      this.notice.set('We could not save the changes. Please try again.');
    }
  }

  requestRemove(productId: string): void {
    this.editingId.set(null);
    this.pendingDeleteId.set(productId);
  }

  cancelRemove(): void {
    this.pendingDeleteId.set(null);
  }

  async confirmRemove(productId: string): Promise<void> {
    const sellerId = this.auth.user()?.id;
    if (sellerId === undefined) return;
    try {
      if (!await this.catalog.removeSellerProduct(productId, sellerId)) return;
      this.pendingDeleteId.set(null);
      this.notice.set('The product listing was removed.');
    } catch {
      this.notice.set('We could not remove the listing. Please try again.');
    }
  }

  private resetForm(): void {
    this.name = '';
    this.description = '';
    this.category = 'Home';
    this.price = null;
    this.stockQuantity = 10;
    this.image = '';
  }
}
