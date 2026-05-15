import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AdminCycleService, WeeklyCycle, AdminOrderService, AdminOrder } from '../services/admin.services';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatSelectModule, MatFormFieldModule, MatProgressSpinnerModule
  ],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss'
})
export class AdminOrdersComponent implements OnInit {
  private cycleService = inject(AdminCycleService);
  private orderService = inject(AdminOrderService);
  private http = inject(HttpClient);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  private base = environment.apiBaseUrl;

  cycles = signal<WeeklyCycle[]>([]);
  orders = signal<AdminOrder[]>([]);
  selectedCycleId = signal<number | null>(null);
  loading = signal(false);

  columns = ['orderNumber', 'phone', 'status', 'payment', 'total', 'slot', 'actions'];
  statusOptions = ['PLACED', 'CONFIRMED', 'PACKED', 'DELIVERED', 'CANCELLED'];

  ngOnInit(): void {
    this.cycleService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => {
        this.cycles.set(c);
        if (c.length > 0) { this.selectedCycleId.set(c[0].id); this.loadOrders(c[0].id); }
      });
  }

  loadOrders(cycleId: number): void {
    this.loading.set(true);
    this.orderService.getAll(cycleId)
      .subscribe({
        next: o => { this.orders.set(o); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  onCycleChange(cycleId: number): void {
    this.selectedCycleId.set(cycleId);
    this.loadOrders(cycleId);
  }

  updateStatus(orderId: number, status: string): void {
    this.orderService.updateStatus(orderId, status).subscribe({
      next: () => { this.snackbar.open('Status updated!', 'Close', { duration: 2000 }); this.loadOrders(this.selectedCycleId()!); }
    });
  }

  markPaid(orderId: number): void {
    this.http.put(`${this.base}/orders/${orderId}/payment`, {}).subscribe({
      next: () => { this.snackbar.open('Marked as Paid!', 'Close', { duration: 2000 }); this.loadOrders(this.selectedCycleId()!); }
    });
  }

  getStatusClass(status: string): string {
    return 'status-' + status.toLowerCase();
  }
}
