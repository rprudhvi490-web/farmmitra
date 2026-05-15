import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { CartService, CartItem } from '../services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatDividerModule],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss'
})
export class CartComponent implements OnInit {
  private cartService = inject(CartService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  items = signal<CartItem[]>([]);

  ngOnInit(): void {
    this.cartService.items
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(items => this.items.set(items));
  }

  adjustQty(item: CartItem, delta: number): void {
    const next = +(item.quantity + delta).toFixed(1);
    if (next < item.minOrderQty) {
      this.cartService.remove(item.productId);
      return;
    }
    this.cartService.addOrUpdate({ ...item, quantity: next });
  }

  remove(productId: number): void {
    this.cartService.remove(productId);
  }

  getTotal(): number {
    return this.cartService.getTotal();
  }

  checkout(): void {
    this.router.navigate(['/customer/checkout']);
  }
}
