import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  sellerId: number;
}

export type FulfillmentStatus = 'PLACED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED';

export interface CustomerOrder {
  reference: string;
  status: FulfillmentStatus;
  createdAt: string;
  total: number;
  items: OrderItem[];
}

export interface SellerOrder {
  reference: string;
  createdAt: string;
  status: FulfillmentStatus;
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  region: string;
  postalCode: string;
  country: string;
  subtotal: number;
  items: Array<{
    productId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    status: FulfillmentStatus;
  }>;
}

export interface PlaceOrderDetails {
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  region: string;
  postalCode: string;
  country: string;
  items: Array<{ productId: string; quantity: number }>;
}

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly http = inject(HttpClient);

  async place(details: PlaceOrderDetails): Promise<CustomerOrder> {
    return firstValueFrom(this.http.post<CustomerOrder>('/api/orders', details));
  }

  async mine(): Promise<CustomerOrder[]> {
    return firstValueFrom(this.http.get<CustomerOrder[]>('/api/orders/mine'));
  }

  async forSeller(): Promise<SellerOrder[]> {
    return firstValueFrom(this.http.get<SellerOrder[]>('/api/orders/seller'));
  }

  async updateSellerStatus(reference: string, status: FulfillmentStatus): Promise<SellerOrder> {
    return firstValueFrom(
      this.http.put<SellerOrder>(`/api/orders/seller/${encodeURIComponent(reference)}/status`, { status }),
    );
  }
}
