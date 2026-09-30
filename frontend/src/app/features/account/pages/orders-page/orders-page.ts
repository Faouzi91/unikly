import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CustomerOrder, OrdersService } from '@core/orders/orders.service';

@Component({
  selector: 'app-orders-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './orders-page.html',
  styleUrl: './orders-page.css',
})
export class OrdersPage {
  private readonly ordersService = inject(OrdersService);
  readonly orders = signal<CustomerOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');
  readonly deliveryError = signal('');
  readonly confirmingReference = signal<string | null>(null);

  constructor() {
    void this.loadOrders();
  }

  async confirmDelivery(order: CustomerOrder): Promise<void> {
    this.confirmingReference.set(order.reference);
    this.deliveryError.set('');
    this.notice.set('');
    try {
      const updated = await this.ordersService.confirmDelivery(order.reference);
      this.orders.update((orders) => orders.map((current) =>
        current.reference === updated.reference ? updated : current,
      ));
      this.notice.set(`Delivery confirmed for order ${updated.reference}.`);
    } catch {
      this.deliveryError.set('We could not confirm delivery. Refresh your orders and try again.');
    } finally {
      this.confirmingReference.set(null);
    }
  }

  async loadOrders(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.orders.set(await this.ordersService.mine());
    } catch {
      this.error.set('We could not load your orders. Please refresh and try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
