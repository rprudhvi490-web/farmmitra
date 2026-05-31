import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OrderService, CustomerOrder } from '../services/customer.services';
import { CartService } from '../services/cart.service';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss'
})
export class OrdersComponent implements OnInit {
  private orderService = inject(OrderService);
  private cartService = inject(CartService);
  private snackbar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  orders = signal<CustomerOrder[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.orderService.getMy()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: o => { this.orders.set(o); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  reorder(order: CustomerOrder): void {
    order.items.forEach(item => {
      this.cartService.addOrUpdate({
        productId: item.productId,
        productName: item.productName,
        unit: item.unit,
        pricePerUnit: item.unitPrice,
        quantity: item.quantity,
        minOrderQty: item.quantity
      });
    });
    this.snackbar.open(`${order.items.length} item(s) added to cart`, 'View Cart', { duration: 3000 })
      .onAction().subscribe(() => this.router.navigate(['/customer/cart']));
    this.router.navigate(['/customer/home']);
  }

  getStatusClass(status: string): string {
    return 'status-' + status.toLowerCase();
  }
}
