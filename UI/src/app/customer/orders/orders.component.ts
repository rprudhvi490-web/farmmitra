import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrderService, CustomerOrder } from '../services/customer.services';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss'
})
export class OrdersComponent implements OnInit {
  private orderService = inject(OrderService);
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

  getStatusClass(status: string): string {
    return 'status-' + status.toLowerCase();
  }
}
