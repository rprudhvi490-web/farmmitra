import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CartService, CartItem } from '../services/cart.service';
import { OrderService, CycleService, WeeklyCycle } from '../services/customer.services';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule
  ],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent implements OnInit {
  private cartService = inject(CartService);
  private orderService = inject(OrderService);
  private cycleService = inject(CycleService);
  private router = inject(Router);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  items = signal<CartItem[]>([]);
  cycle = signal<WeeklyCycle | null>(null);
  loading = signal(false);
  placing = signal(false);

  notes = new FormControl('');

  ngOnInit(): void {
    this.items.set(this.cartService.getItems());

    if (this.items().length === 0) {
      this.router.navigate(['/customer/cart']);
      return;
    }

    this.loading.set(true);
    this.cycleService.getCurrent()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: c => { this.cycle.set(c); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  getTotal(): number {
    return this.cartService.getTotal();
  }

  placeOrder(): void {
    if (!this.cycle() || this.cycle()!.status !== 'OPEN') {
      this.snackbar.open('Ordering is currently closed.', 'Close', { duration: 3000 });
      return;
    }

    this.placing.set(true);
    const req = {
      items: this.items().map(i => ({ productId: i.productId, quantity: i.quantity })),
      notes: this.notes.value ?? ''
    };

    this.orderService.place(req).subscribe({
      next: (order) => {
        this.cartService.clear();
        this.placing.set(false);
        this.snackbar.open(`Order ${order.orderNumber} placed!`, 'Close', { duration: 3000 });
        this.router.navigate(['/customer/orders']);
      },
      error: () => this.placing.set(false)
    });
  }
}
