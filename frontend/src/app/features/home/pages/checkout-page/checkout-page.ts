import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/identity/auth.service';
import { CartService } from '@core/cart/cart.service';
import { ProductCatalogService } from '../../data/product-catalog.service';
import { CustomerOrder, OrdersService } from '@core/orders/orders.service';

@Component({
  selector: 'app-checkout-page',
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './checkout-page.html',
  styleUrl: './checkout-page.css',
})
export class CheckoutPage {
  readonly cart = inject(CartService);
  private readonly catalog = inject(ProductCatalogService);
  readonly auth = inject(AuthService);
  private readonly orders = inject(OrdersService);
  readonly items = computed(() => this.cart.lines().map((line) => {
    const product = this.catalog.products().find((item) => item.id === line.productId);
    return {
      ...line,
      name: product?.name ?? line.productId,
      price: product?.price ?? 0,
      stockQuantity: product?.stockQuantity ?? 0,
      available: product !== undefined,
    };
  }));
  readonly subtotal = computed(() => this.items().reduce((sum, item) => sum + item.price * item.quantity, 0));
  readonly buyerSignedIn = computed(() => this.auth.user()?.role === 'BUYER');
  readonly placedOrder = signal<CustomerOrder | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly sessionExpired = signal(false);

  fullName = '';
  email = '';
  phone = '';
  addressLine1 = '';
  addressLine2 = '';
  city = '';
  region = '';
  postalCode = '';
  country = '';

  constructor() {
    this.email = this.auth.user()?.email ?? '';
    this.fullName = this.auth.user()?.displayName ?? '';
  }

  async placeDemoOrder(invalid: boolean | null): Promise<void> {
    if (invalid || this.busy() || this.items().length === 0 || this.auth.user()?.role !== 'BUYER') return;
    const unavailable = this.items().find((item) => !item.available || item.stockQuantity < item.quantity);
    if (unavailable) {
      this.error.set(`${unavailable.name} no longer has enough stock. Return to your basket to remove it.`);
      return;
    }
    this.error.set('');
    this.busy.set(true);
    try {
      const order = await this.orders.place({
        fullName: this.fullName.trim(), email: this.email.trim(), phone: this.phone.trim(),
        addressLine1: this.addressLine1.trim(), addressLine2: this.addressLine2.trim(),
        city: this.city.trim(), region: this.region.trim(), postalCode: this.postalCode.trim(),
        country: this.country.trim(),
        items: this.items().map(({ productId, quantity }) => ({ productId, quantity })),
      });
      this.placedOrder.set(order);
      this.cart.clear();
      await this.catalog.refresh();
    } catch (error: unknown) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        this.auth.expireSession();
        this.sessionExpired.set(true);
        this.error.set('Your sign-in session expired. Sign in again to place this order; your basket is still saved.');
        return;
      }
      const response = error as { status?: number; error?: { detail?: string; message?: string } };
      const serverMessage = response.error?.detail ?? response.error?.message;
      this.error.set(serverMessage ?? (response.status === 404
        ? 'A product in your basket is no longer available. Remove it and try again.'
        : response.status === 409
          ? 'Stock changed while you were checking out. Review your basket and try again.'
          : 'We could not place your order. Check your connection and try again.'));
    } finally {
      this.busy.set(false);
    }
  }
}
