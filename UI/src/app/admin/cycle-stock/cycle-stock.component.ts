import { Component, inject, signal, OnInit } from '@angular/core';
import { catchError, of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  AdminCycleService, WeeklyCycle,
  CycleProductAdminService, CycleProductResponse, StockSuggestion
} from '../services/admin.services';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-cycle-stock',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule, MatTooltipModule
  ],
  templateUrl: './cycle-stock.component.html',
  styleUrl: './cycle-stock.component.scss'
})
export class CycleStockComponent implements OnInit {
  private cycleService = inject(AdminCycleService);
  private stockService = inject(CycleProductAdminService);
  private toast = inject(ToastService);

  cycles = signal<WeeklyCycle[]>([]);
  selectedCycleId = signal<number | null>(null);
  rows = signal<CycleProductResponse[]>([]);
  suggestions = signal<StockSuggestion[]>([]);
  loading = signal(false);
  saving = signal(false);

  editValues: Record<number, number | null> = {};
  cachedDisplayRows: any[] = [];
  columns = ['product', 'unit', 'maxStock', 'orderedQty', 'remainingQty', 'soldOut'];

  ngOnInit(): void {
    this.stockService.getSuggestions()
      .pipe(catchError(() => of([] as StockSuggestion[])))
      .subscribe({
        next: s => { this.suggestions.set(s); this.updateDisplayRows(); }
      });

    this.cycleService.getAll()
      .pipe(catchError(() => of([] as WeeklyCycle[])))
      .subscribe({
        next: cycles => {
          this.cycles.set([...cycles].reverse());
          const target = cycles.find(c => c.status === 'OPEN') ?? cycles[cycles.length - 1];
          if (target) { this.selectedCycleId.set(target.id); this.load(target.id); }
        }
      });
  }

  onCycleChange(cycleId: number): void {
    this.selectedCycleId.set(cycleId);
    this.load(cycleId);
  }

  load(cycleId: number): void {
    this.loading.set(true);
    this.stockService.getForCycle(cycleId)
      .pipe(catchError(() => of([] as CycleProductResponse[])))
      .subscribe({
        next: rows => {
          const safe = rows ?? [];
          this.rows.set(safe);
          this.editValues = {};
          safe.forEach(r => this.editValues[r.productId] = r.maxStock);
          this.suggestions().forEach(s => {
            if (!(s.productId in this.editValues)) {
              this.editValues[s.productId] = s.suggestedMaxStock;
            }
          });
          this.updateDisplayRows();
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  private updateDisplayRows(): void {
    const configured = this.rows();
    const configuredIds = new Set(configured.map(r => r.productId));
    const unconfigured = this.suggestions()
      .filter(s => !configuredIds.has(s.productId))
      .map(s => ({
        productId: s.productId,
        productName: s.productName,
        unit: s.unit,
        maxStock: null,
        orderedQty: 0,
        remainingQty: null,
        soldOut: false,
        unsaved: true
      }));
    this.cachedDisplayRows = [...configured, ...unconfigured];
  }

  saveAll(): void {
    const cycleId = this.selectedCycleId();
    if (!cycleId) return;

    const items = Object.entries(this.editValues)
      .filter(([, v]) => v !== null && v !== undefined && Number(v) > 0)
      .map(([productId, maxStock]) => ({ productId: Number(productId), maxStock: Number(maxStock) }));

    if (!items.length) {
      this.toast.warning('No stock limits to save.');
      return;
    }

    this.saving.set(true);
    this.stockService.bulkSetStock(cycleId, { items })
      .pipe(catchError(() => of([] as CycleProductResponse[])))
      .subscribe({
        next: rows => {
          this.rows.set(rows);
          rows.forEach(r => this.editValues[r.productId] = r.maxStock);
          this.updateDisplayRows();
          this.saving.set(false);
          this.toast.success('Stock limits saved!');
        },
        error: () => this.saving.set(false)
      });
  }

  applySuggestionsToAll(): void {
    this.suggestions().forEach(s => {
      if (s.suggestedMaxStock !== null) {
        this.editValues[s.productId] = s.suggestedMaxStock;
      }
    });
    this.toast.info('Suggestions applied — review and save.');
  }
}
