import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FulfillmentStatus, OrdersService, SellerOrder } from '@core/orders/orders.service';

@Component({
  selector: 'app-seller-orders-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './seller-orders-page.html',
  styleUrl: './seller-orders-page.css',
})
export class SellerOrdersPage {
  private readonly ordersService = inject(OrdersService);
  readonly orders = signal<SellerOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');
  readonly updatingReference = signal<string | null>(null);

  constructor() {
    void this.loadOrders();
  }

  async loadOrders(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.orders.set(await this.ordersService.forSeller());
    } catch {
      this.error.set('We could not load your orders. Please refresh and try again.');
    } finally {
      this.loading.set(false);
    }
  }

  nextStatus(status: FulfillmentStatus): FulfillmentStatus | null {
    switch (status) {
      case 'PLACED': return 'PROCESSING';
      case 'PROCESSING': return 'SHIPPED';
      case 'SHIPPED': return 'DELIVERED';
      case 'DELIVERED': return null;
    }
  }

  actionLabel(status: FulfillmentStatus): string {
    switch (status) {
      case 'PLACED': return 'Start processing';
      case 'PROCESSING': return 'Mark as shipped';
      case 'SHIPPED': return 'Mark delivered';
      case 'DELIVERED': return 'Fulfillment complete';
    }
  }

  async advance(order: SellerOrder): Promise<void> {
    const status = this.nextStatus(order.status);
    if (!status) return;
    this.updatingReference.set(order.reference);
    this.error.set('');
    this.notice.set('');
    try {
      const updated = await this.ordersService.updateSellerStatus(order.reference, status);
      this.orders.update((orders) =>
        orders.map((current) => current.reference === updated.reference ? updated : current),
      );
      this.notice.set(`Order ${updated.reference} updated to ${updated.status.toLowerCase()}.`);
    } catch {
      this.error.set('We could not update this order. Refresh the page and try again.');
    } finally {
      this.updatingReference.set(null);
    }
  }
}
