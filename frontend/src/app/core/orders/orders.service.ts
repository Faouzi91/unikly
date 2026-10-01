import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  sellerId: number | null;
  status: FulfillmentStatus;
  carrierName: string | null;
  trackingUrl: string | null;
}

export type FulfillmentStatus = 'PLACED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELED';
export type DeliveryMethod = 'STANDARD' | 'EXPRESS';

export interface CustomerOrder {
  reference: string;
  status: FulfillmentStatus;
  createdAt: string;
  deliveryMethod?: DeliveryMethod;
  deliveryFee?: number;
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
  deliveryMethod?: DeliveryMethod;
  subtotal: number;
  items: Array<{
    productId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    status: FulfillmentStatus;
    carrierName: string | null;
    trackingUrl: string | null;
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
  deliveryMethod?: DeliveryMethod;
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

  async confirmDelivery(reference: string): Promise<CustomerOrder> {
    return firstValueFrom(
      this.http.put<CustomerOrder>(`/api/orders/mine/${encodeURIComponent(reference)}/confirm-delivery`, {}),
    );
  }

  async cancel(reference: string): Promise<CustomerOrder> {
    return firstValueFrom(
      this.http.put<CustomerOrder>(`/api/orders/mine/${encodeURIComponent(reference)}/cancel`, {}),
    );
  }

  async forSeller(): Promise<SellerOrder[]> {
    return firstValueFrom(this.http.get<SellerOrder[]>('/api/orders/seller'));
  }

  async updateSellerStatus(reference: string, status: FulfillmentStatus,
                           shipping?: { carrierName: string; trackingUrl: string }): Promise<SellerOrder> {
    return firstValueFrom(
      this.http.put<SellerOrder>(`/api/orders/seller/${encodeURIComponent(reference)}/status`, { status, ...shipping }),
    );
  }
}
