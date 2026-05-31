import { Component, inject, signal, Input, OnInit, DestroyRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrderService, CustomerOrder } from '../../services/customer.services';
import { CartService } from '../../services/cart.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface TransportStage {
  id: number;
  stage: string;
  notes: string;
  updatedBy: string;
  createdOn: string;
}

const STAGE_LABELS: Record<string, string> = {
  PROCUREMENT_STARTED: 'Procurement Started',
  GOODS_LOADED: 'Goods Loaded',
  IN_TRANSIT: 'In Transit to Hyderabad',
  ARRIVED: 'Arrived in Hyderabad',
  PACKING: 'Packing in Progress',
  DISPATCHED: 'Out for Delivery'
};

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss'
})
export class OrderDetailComponent implements OnInit {
  @Input() id!: string;

  private orderService = inject(OrderService);
  private cartService = inject(CartService);
  private http = inject(HttpClient);
  private snackbar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private base = environment.apiBaseUrl;

  order = signal<CustomerOrder | null>(null);
  stages = signal<TransportStage[]>([]);
  loading = signal(true);
  cancelling = signal(false);

  ngOnInit(): void {
    this.orderService.getMyById(+this.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: o => {
          this.order.set(o);
          this.loading.set(false);
          if (o.cycleId) {
            this.http.get<TransportStage[]>(`${this.base}/transport/${o.cycleId}`)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe(s => this.stages.set(s));
          }
        },
        error: () => this.loading.set(false)
      });
  }

  reorder(): void {
    const o = this.order();
    if (!o) return;
    o.items.forEach(item => {
      this.cartService.addOrUpdate({
        productId: item.productId,
        productName: item.productName,
        unit: item.unit,
        pricePerUnit: item.unitPrice,
        quantity: item.quantity,
        minOrderQty: item.quantity
      });
    });
    this.snackbar.open(`${o.items.length} item(s) added to cart`, 'View Cart', { duration: 3000 })
      .onAction().subscribe(() => this.router.navigate(['/customer/cart']));
    this.router.navigate(['/customer/home']);
  }

  getStageLabel(stage: string): string {
    return STAGE_LABELS[stage] ?? stage;
  }

  cancel(): void {
    this.cancelling.set(true);
    this.orderService.cancel(+this.id).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.snackbar.open('Order cancelled', 'Close', { duration: 3000 });
        this.orderService.getMyById(+this.id).subscribe(o => this.order.set(o));
      },
      error: () => this.cancelling.set(false)
    });
  }

  getStatusClass(status: string): string {
    return 'status-' + status.toLowerCase();
  }
}
