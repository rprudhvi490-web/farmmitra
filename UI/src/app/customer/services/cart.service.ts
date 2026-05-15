import { Injectable, signal, computed } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface CartItem {
  productId: number;
  productName: string;
  unit: string;
  pricePerUnit: number;
  quantity: number;
  totalPrice: number;
  minOrderQty: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private items$ = new BehaviorSubject<CartItem[]>([]);

  readonly items = this.items$.asObservable();

  getItems(): CartItem[] {
    return this.items$.getValue();
  }

  getCount(): number {
    return this.items$.getValue().length;
  }

  getTotal(): number {
    return this.items$.getValue().reduce((sum, i) => sum + i.totalPrice, 0);
  }

  addOrUpdate(item: Omit<CartItem, 'totalPrice'>): void {
    const current = this.items$.getValue();
    const existing = current.findIndex(i => i.productId === item.productId);
    if (existing >= 0) {
      const updated = [...current];
      updated[existing] = { ...item, totalPrice: item.pricePerUnit * item.quantity };
      this.items$.next(updated);
    } else {
      this.items$.next([...current, { ...item, totalPrice: item.pricePerUnit * item.quantity }]);
    }
  }

  remove(productId: number): void {
    this.items$.next(this.items$.getValue().filter(i => i.productId !== productId));
  }

  clear(): void {
    this.items$.next([]);
  }
}
