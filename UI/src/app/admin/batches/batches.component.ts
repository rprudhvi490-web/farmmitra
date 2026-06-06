import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AdminCycleService, AdminOrderService, AdminUserService, WeeklyCycle, AdminOrder, AdminUser } from '../services/admin.services';

interface Batch {
  id: number;
  batchLabel: string;
  cycleId: number;
  cycleLabel: string;
  deliveryDate: string;
  assignedToUserId: number | null;
  assignedToUsername: string | null;
  status: string;
  orders: BatchOrder[];
}

interface BatchOrder {
  orderId: number;
  orderNumber: string;
  customerName: string;
  flatNumber: string;
  block: string;
  deliverySlot: string;
  orderStatus: string;
}

@Component({
  selector: 'app-batches',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatExpansionModule, MatProgressSpinnerModule
  ],
  templateUrl: './batches.component.html',
  styleUrl: './batches.component.scss'
})
export class BatchesComponent implements OnInit {
  private http = inject(HttpClient);
  private cycleService = inject(AdminCycleService);
  private orderService = inject(AdminOrderService);
  private userService = inject(AdminUserService);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  private base = environment.apiBaseUrl;

  cycles = signal<WeeklyCycle[]>([]);
  batches = signal<Batch[]>([]);
  orders = signal<AdminOrder[]>([]);
  deliveryStaff = signal<AdminUser[]>([]);
  selectedCycleId = signal<number | null>(null);
  loading = signal(false);
  showCreateForm = signal(false);
  saving = signal(false);

  createForm = new FormGroup({
    batchLabel:   new FormControl('', Validators.required),
    deliveryDate: new FormControl('', Validators.required),
    assignedToUserId: new FormControl<number | null>(null),
    orderIds:     new FormControl<number[]>([], Validators.required)
  });

  ngOnInit(): void {
    // Load cycles
    this.cycleService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => {
        const sorted = [...c].reverse();
        this.cycles.set(sorted);
        if (sorted.length > 0) { this.selectedCycleId.set(sorted[0].id); this.loadData(sorted[0].id); }
      });

    // Load delivery staff
    this.userService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(users => {
        this.deliveryStaff.set(users.filter(u => u.roles.includes('ROLE_DELIVERY') || u.roles.includes('ROLE_ADMIN')));
      });
  }

  loadData(cycleId: number): void {
    this.loading.set(true);
    this.http.get<Batch[]>(`${this.base}/delivery/batches/cycle/${cycleId}`)
      .subscribe({ next: b => { this.batches.set(b); this.loading.set(false); }, error: () => this.loading.set(false) });

    this.orderService.getAll(cycleId)
      .subscribe(o => this.orders.set(o.filter(order => order.status !== 'CANCELLED')));
  }

  onCycleChange(cycleId: number): void {
    this.selectedCycleId.set(cycleId);
    this.loadData(cycleId);
  }

  createBatch(): void {
    if (this.createForm.invalid) { this.createForm.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.createForm.value;
    const req = {
      cycleId: this.selectedCycleId(),
      batchLabel: v.batchLabel,
      deliveryDate: v.deliveryDate,
      assignedToUserId: v.assignedToUserId,
      orderIds: v.orderIds
    };
    this.http.post(`${this.base}/delivery/batches`, req).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCreateForm.set(false);
        this.createForm.reset();
        this.snackbar.open('Batch created!', 'Close', { duration: 2000 });
        this.loadData(this.selectedCycleId()!);
      },
      error: () => this.saving.set(false)
    });
  }

  markDelivered(batchId: number, orderId: number): void {
    this.http.put(`${this.base}/delivery/batches/${batchId}/orders/${orderId}/delivered`, {}).subscribe({
      next: () => {
        this.snackbar.open('Order marked as delivered!', 'Close', { duration: 2000 });
        this.loadData(this.selectedCycleId()!);
      }
    });
  }

  assignStaff(batchId: number, userId: number): void {
    this.http.put(`${this.base}/delivery/batches/${batchId}/assign?staffUserId=${userId}`, {}).subscribe({
      next: () => this.snackbar.open('Staff assigned!', 'Close', { duration: 2000 })
    });
  }

  getUnassignedOrders(): AdminOrder[] {
    const assignedIds = new Set(this.batches().flatMap(b => b.orders.map(o => o.orderId)));
    return this.orders().filter(o => !assignedIds.has(o.id));
  }
}
