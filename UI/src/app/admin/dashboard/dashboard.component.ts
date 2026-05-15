import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  DashboardService, DashboardSummary,
  CycleHistoryResponse, CustomerAnalyticsResponse
} from '../services/admin.services';
import { AdminCycleService, WeeklyCycle } from '../services/admin.services';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    RouterLink, CommonModule,
    MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule,
    MatTableModule, MatTabsModule, MatChipsModule, MatTooltipModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private cycleService = inject(AdminCycleService);
  private destroyRef = inject(DestroyRef);

  summary = signal<DashboardSummary | null>(null);
  cycle = signal<WeeklyCycle | null>(null);
  history = signal<CycleHistoryResponse | null>(null);
  customers = signal<CustomerAnalyticsResponse | null>(null);
  loading = signal(true);

  cycleColumns = ['cycleLabel', 'status', 'totalOrders', 'uniqueCustomers', 'totalRevenue', 'collectedRevenue', 'pendingRevenue'];
  customerColumns = ['username', 'phone', 'flat', 'cyclesParticipated', 'totalOrders', 'totalSpent', 'totalPaid', 'loyaltyTag'];

  ngOnInit(): void {
    this.cycleService.getCurrent()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => this.cycle.set(c));

    this.dashboardService.getSummary()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: s => { this.summary.set(s); this.loading.set(false); }, error: () => this.loading.set(false) });

    this.dashboardService.getHistory()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(h => this.history.set(h));

    this.dashboardService.getCustomerAnalytics()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => this.customers.set(c));
  }

  loyaltyColor(tag: string): string {
    return ({ NEW: 'default', REGULAR: 'primary', LOYAL: 'accent', CHAMPION: 'warn' } as any)[tag] ?? 'default';
  }
}
