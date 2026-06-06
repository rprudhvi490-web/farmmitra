import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface WeeklyCycle {
  id: number;
  cycleLabel: string;
  status: string;
  orderOpenAt: string;
  orderCloseAt: string;
  deliveryDateSat: string;
  deliveryDateSun: string;
  timeRemainingSeconds: number | null;
}

export interface CycleHistoryEntry {
  cycleId: number;
  cycleLabel: string;
  status: string;
  orderOpenAt: string;
  orderCloseAt: string;
  totalOrders: number;
  cancelledOrders: number;
  deliveredOrders: number;
  uniqueCustomers: number;
  totalRevenue: number;
  collectedRevenue: number;
  pendingRevenue: number;
}

export interface CycleHistoryResponse {
  cycles: CycleHistoryEntry[];
  totalCycles: number;
  allTimeRevenue: number;
  allTimeCollected: number;
  allTimePending: number;
  allTimeOrders: number;
  allTimeUniqueCustomers: number;
}

export interface CustomerOrderSummary {
  userId: number;
  phoneNumber: string;
  username: string;
  flatNumber: string;
  block: string;
  totalOrders: number;
  cyclesParticipated: number;
  totalSpent: number;
  totalPaid: number;
  loyaltyTag: 'NEW' | 'REGULAR' | 'LOYAL' | 'CHAMPION';
}

export interface CustomerAnalyticsResponse {
  customers: CustomerOrderSummary[];
  totalCustomers: number;
  newCustomers: number;
  regularCustomers: number;
  loyalCustomers: number;
  championCustomers: number;
}

export interface DashboardSummary {
  totalUsers: number;
  totalOrders: number;
  totalDeliveredOrders: number;
  totalRevenue: number;
  totalProducts: number;
  activeProducts: number;
  totalCategories: number;
  currentCycle: {
    cycleId: number;
    cycleLabel: string;
    status: string;
    totalOrders: number;
    cancelledOrders: number;
    deliveredOrders: number;
    totalRevenue: number;
    totalProcurementItems: number;
    pendingProcurementItems: number;
  } | null;
}

export interface AdminOrder {
  id: number;
  orderNumber: string;
  phoneNumber: string;
  status: string;
  paymentStatus: string;
  totalAmount: number;
  amountToCollect: number;
  deliverySlot: string;
  createdOn: string;
  items: any[];
}

export interface ProcurementItem {
  id: number;
  productId: number;
  productName: string;
  unit: string;
  totalQuantity: number;
  vendorName: string;
  vendorNotes: string;
  procuredQty: number;
  status: string;
}

export interface ProcurementSheet {
  cycleId: number;
  cycleLabel: string;
  totalOrders: number;
  items: ProcurementItem[];
}

export interface TransportStage {
  id: number;
  stage: string;
  notes: string;
  updatedBy: string;
  createdOn: string;
}

export interface AdminUser {
  userId: number;
  phoneNumber: string;
  username: string;
  status: string;
  roles: string[];
}

// ── Cycle Service ─────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class AdminCycleService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getAll(): Observable<WeeklyCycle[]> {
    return this.http.get<WeeklyCycle[]>(`${this.base}/cycles`);
  }

  getCurrent(): Observable<WeeklyCycle | null> {
    return this.http.get<WeeklyCycle | null>(`${this.base}/cycles/current`);
  }

  create(req: any): Observable<WeeklyCycle> {
    return this.http.post<WeeklyCycle>(`${this.base}/cycles`, req);
  }

  open(id: number): Observable<any> {
    return this.http.put(`${this.base}/cycles/${id}/open`, {});
  }

  close(id: number): Observable<any> {
    return this.http.put(`${this.base}/cycles/${id}/close`, {});
  }

  updateStatus(id: number, status: string): Observable<any> {
    return this.http.put(`${this.base}/cycles/${id}/status`, { status });
  }
}

// ── Dashboard Service ─────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.base}/dashboard/summary`);
  }

  getHistory(): Observable<CycleHistoryResponse> {
    return this.http.get<CycleHistoryResponse>(`${this.base}/dashboard/history`);
  }

  getCustomerAnalytics(): Observable<CustomerAnalyticsResponse> {
    return this.http.get<CustomerAnalyticsResponse>(`${this.base}/dashboard/customers`);
  }
}

// ── Admin Order Service ───────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class AdminOrderService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getAll(cycleId?: number): Observable<AdminOrder[]> {
    const url = cycleId
      ? `${this.base}/orders/cycle/${cycleId}`
      : `${this.base}/orders/cycle/0`;
    return this.http.get<any>(url).pipe(
      map((page: any) => page.content ?? page)
    );
  }

  getById(id: number): Observable<AdminOrder> {
    return this.http.get<AdminOrder>(`${this.base}/orders/${id}`);
  }

  updateStatus(id: number, status: string): Observable<any> {
    return this.http.put(`${this.base}/orders/${id}/status`, { status });
  }

  adminCancel(id: number): Observable<any> {
    return this.http.put(`${this.base}/orders/${id}/cancel`, {});
  }
}

// ── Procurement Service ───────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class ProcurementService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getSheet(cycleId: number): Observable<ProcurementSheet> {
    return this.http.get<ProcurementSheet>(`${this.base}/procurement/${cycleId}`);
  }

  update(cycleId: number, productId: number, req: any): Observable<any> {
    return this.http.put(`${this.base}/procurement/items/${productId}`, req);
  }

  markAllProcured(cycleId: number): Observable<any> {
    return this.http.put(`${this.base}/procurement/${cycleId}/mark-all-procured`, {});
  }

  export(cycleId: number): Observable<Blob> {
    return this.http.get(`${this.base}/procurement/${cycleId}/export`, { responseType: 'blob' });
  }
}

// ── Transport Service ─────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class TransportService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getLog(cycleId: number): Observable<TransportStage[]> {
    return this.http.get<TransportStage[]>(`${this.base}/transport/${cycleId}`);
  }

  addStage(cycleId: number, req: any): Observable<any> {
    return this.http.post(`${this.base}/transport/${cycleId}/stage`, req);
  }
}

// ── Notification Admin Service ───────────────────────────────
@Injectable({ providedIn: 'root' })
export class NotificationAdminService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  broadcast(req: { title: string; body: string; type: string }): Observable<any> {
    return this.http.post(`${this.base}/notifications/broadcast`, req);
  }

  sendToUser(req: { userId: number | null; title: string; body: string; type: string }): Observable<any> {
    return this.http.post(`${this.base}/notifications/send`, req);
  }
}

// ── Cycle Product Service ────────────────────────────────────
export interface CycleProductResponse {
  id: number;
  productId: number;
  productName: string;
  unit: string;
  maxStock: number;
  orderedQty: number;
  remainingQty: number;
  soldOut: boolean;
}

export interface StockSuggestion {
  productId: number;
  productName: string;
  unit: string;
  suggestedMaxStock: number | null;
}

export interface BulkSetStockRequest {
  items: { productId: number; maxStock: number }[];
}

@Injectable({ providedIn: 'root' })
export class CycleProductAdminService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getForCycle(cycleId: number): Observable<CycleProductResponse[]> {
    return this.http.get<CycleProductResponse[]>(`${this.base}/cycle-products/${cycleId}`).pipe(
      map(res => res ?? [])
    );
  }

  getSuggestions(): Observable<StockSuggestion[]> {
    return this.http.get<StockSuggestion[]>(`${this.base}/cycle-products/suggestions`);
  }

  bulkSetStock(cycleId: number, req: BulkSetStockRequest): Observable<CycleProductResponse[]> {
    return this.http.put<CycleProductResponse[]>(`${this.base}/cycle-products/${cycleId}`, req);
  }
}

// ── Admin User Service ────────────────────────────────────────
export interface UserSession {
  issuedAt: string;
  lastUsedAt: string;
  expiredAt: string;
  deviceHint: string | null;
  activeNow: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getAll(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.base}/users`);
  }

  block(id: number): Observable<any> {
    return this.http.put(`${this.base}/users/${id}/block`, {});
  }

  unblock(id: number): Observable<any> {
    return this.http.put(`${this.base}/users/${id}/unblock`, {});
  }

  assignRole(id: number, roleId: string): Observable<any> {
    return this.http.post(`${this.base}/users/${id}/roles`, { roleId });
  }

  removeRole(id: number, roleId: string): Observable<any> {
    return this.http.delete(`${this.base}/users/${id}/roles`, { body: { roleId } });
  }

  getSessions(id: number): Observable<UserSession[]> {
    return this.http.get<UserSession[]>(`${this.base}/users/${id}/sessions`);
  }
}
