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

  constructor() {
    void this.loadOrders();
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
