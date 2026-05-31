import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Models ────────────────────────────────────────────────────

export interface WeeklyCycle {
  id: number;
  cycleLabel: string;
  status: 'OPEN' | 'CLOSED' | 'PROCUREMENT' | 'DELIVERING' | 'COMPLETED';
  orderOpenAt: string;
  orderCloseAt: string;
  deliveryDateSat: string;
  deliveryDateSun: string;
  timeRemainingSeconds: number | null;
}

export interface Category {
  id: number;
  name: string;
  imageUrl: string;
  displayOrder: number;
  active: boolean;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  unit: string;
  pricePerUnit: number;
  imageUrl: string;
  available: boolean;
  minOrderQty: number;
  rating: number;
}

export interface OrderItem {
  productId: number;
  quantity: number;
}

export interface PlaceOrderRequest {
  items: OrderItem[];
  notes?: string;
}

export interface CustomerOrder {
  id: number;
  orderNumber: string;
  cycleId: number;
  cycleLabel: string;
  status: string;
  totalAmount: number;
  referralDiscount: number;
  amountToCollect: number;
  paymentMethod: string;
  paymentStatus: string;
  deliverySlot: string | null;
  notes: string;
  createdOn: string;
  items: OrderItemDetail[];
}

export interface OrderItemDetail {
  productId: number;
  productName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

// ── Services ──────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class CycleService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getCurrent(): Observable<WeeklyCycle | null> {
    return this.http.get<WeeklyCycle | null>(`${this.base}/cycles/current`);
  }
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/categories`);
  }

  getAll(categoryId?: number, search?: string): Observable<Product[]> {
    let url = `${this.base}/products`;
    const params: string[] = [];
    if (categoryId) params.push(`category=${categoryId}`);
    if (search) params.push(`search=${encodeURIComponent(search)}`);
    if (params.length) url += '?' + params.join('&');
    return this.http.get<Product[]>(url);
  }

  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.base}/products/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  place(req: PlaceOrderRequest): Observable<CustomerOrder> {
    return this.http.post<CustomerOrder>(`${this.base}/orders`, req);
  }

  getMy(): Observable<CustomerOrder[]> {
    return this.http.get<CustomerOrder[]>(`${this.base}/orders/my`);
  }

  getMyById(id: number): Observable<CustomerOrder> {
    return this.http.get<CustomerOrder>(`${this.base}/orders/my/${id}`);
  }

  cancel(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/orders/${id}/cancel`, {});
  }
}
