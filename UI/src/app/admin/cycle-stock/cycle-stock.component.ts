import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  AdminCycleService, WeeklyCycle,
  CycleProductAdminService, CycleProductResponse, StockSuggestion
} from '../services/admin.services';

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
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  cycles = signal<WeeklyCycle[]>([]);
  selectedCycleId = signal<number | null>(null);
  rows = signal<CycleProductResponse[]>([]);
  suggestions = signal<StockSuggestion[]>([]);
  loading = signal(false);
  saving = signal(false);

  // productId → editable maxStock value
  editValues: Record<number, number | null> = {};

  columns = ['product', 'unit', 'maxStock', 'orderedQty', 'remainingQty', 'soldOut'];

  ngOnInit(): void {
    this.cycleService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(cycles => {
        this.cycles.set(cycles);
        // Default to OPEN cycle, else first
        const open = cycles.find(c => c.status === 'OPEN') ?? cycles[0];
        if (open) { this.selectedCycleId.set(open.id); this.load(open.id); }
      });

    this.stockService.getSuggestions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: s => this.suggestions.set(s),
        error: () => this.suggestions.set([])
      });
  }

  onCycleChange(cycleId: number): void {
    this.selectedCycleId.set(cycleId);
    this.load(cycleId);
  }

  load(cycleId: number): void {
    this.loading.set(true);
    this.stockService.getForCycle(cycleId)
      .pipe(
        catchError(() => of([]))
      )
      .subscribe(rows => {
        this.rows.set(rows);
        this.loading.set(false);
        rows.forEach(r => this.editValues[r.productId] = r.maxStock);
        this.applySuggestions(rows);
        this.suggestions().forEach(s => {
          if (!(s.productId in this.editValues)) {
            this.editValues[s.productId] = s.suggestedMaxStock;
          }
        });
      });
  }

  private applySuggestions(existing: CycleProductResponse[]): void {
    const existingIds = new Set(existing.map(r => r.productId));
    this.suggestions()
      .filter(s => !existingIds.has(s.productId))
      .forEach(s => { this.editValues[s.productId] = s.suggestedMaxStock; });
  }

  // Rows to display: configured rows + unconfigured suggestions
  get displayRows(): any[] {
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
    return [...configured, ...unconfigured];
  }

  saveAll(): void {
    const cycleId = this.selectedCycleId();
    if (!cycleId) return;

    const items = Object.entries(this.editValues)
      .filter(([, v]) => v !== null && v !== undefined && Number(v) > 0)
      .map(([productId, maxStock]) => ({ productId: Number(productId), maxStock: Number(maxStock) }));

    if (!items.length) {
      this.snackbar.open('No stock limits to save.', 'Close', { duration: 2000 });
      return;
    }

    this.saving.set(true);
    this.stockService.bulkSetStock(cycleId, { items })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rows => {
          this.rows.set(rows);
          rows.forEach(r => this.editValues[r.productId] = r.maxStock);
          this.saving.set(false);
          this.snackbar.open('Stock limits saved!', 'Close', { duration: 2500 });
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
    this.snackbar.open('Suggestions applied — review and save.', 'Close', { duration: 2500 });
  }
}
