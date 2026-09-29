import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/identity/auth.service';
import { ProductCatalogService } from '../../data/product-catalog.service';
import { StoreProduct } from '../../data/sample-products';

@Component({
  selector: 'app-seller-product-page',
  imports: [FormsModule, RouterLink],
  templateUrl: './seller-product-page.html',
  styleUrl: './seller-product-page.css',
})
export class SellerProductPage {
  private readonly auth = inject(AuthService);
  private readonly catalog = inject(ProductCatalogService);
  readonly categories = ['Home', 'Electronics', 'Kitchen', 'Outdoor'];
  readonly createdProduct = signal<StoreProduct | null>(null);
  name = '';
  description = '';
  category = 'Home';
  price: number | null = null;
  stockQuantity = 10;
  image = '';
  readonly error = signal('');

  async submit(): Promise<void> {
    const seller = this.auth.user();
    if (seller?.role !== 'SELLER' || this.price === null || this.price <= 0) return;

    try {
      this.createdProduct.set(await this.catalog.addProduct(
        {
          name: this.name.trim(),
          description: this.description.trim(),
          category: this.category,
          price: this.price,
          stockQuantity: this.stockQuantity,
          image: this.image.trim(),
        },
        seller.id,
      ));
      this.error.set('');
      this.name = '';
    this.description = '';
    this.category = 'Home';
      this.price = null;
      this.stockQuantity = 10;
      this.image = '';
    } catch {
      this.error.set('We could not save your product. Please check your connection and try again.');
    }
  }
}
