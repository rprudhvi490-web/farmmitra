import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
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
import { ProcurementService, ProcurementSheet, ProcurementItem, AdminCycleService, WeeklyCycle } from '../services/admin.services';

@Component({
  selector: 'app-procurement',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule
  ],
  templateUrl: './procurement.component.html',
  styleUrl: './procurement.component.scss'
})
export class ProcurementComponent implements OnInit {
  private procurementService = inject(ProcurementService);
  private cycleService = inject(AdminCycleService);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  cycles = signal<WeeklyCycle[]>([]);
  sheet = signal<ProcurementSheet | null>(null);
  selectedCycleId = signal<number | null>(null);
  loading = signal(false);
  exporting = signal(false);
  markingAll = signal(false);

  columns = ['product', 'unit', 'totalQty', 'vendor', 'procuredQty', 'status', 'actions'];

  // Track inline edit values
  editValues: Record<number, { vendorName: string; vendorNotes: string; procuredQty: number; status: string }> = {};

  ngOnInit(): void {
    this.cycleService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => {
        this.cycles.set(c);
        const closed = c.find(x => x.status !== 'OPEN');
        const target = closed ?? c[0];
        if (target) { this.selectedCycleId.set(target.id); this.loadSheet(target.id); }
      });
  }

  loadSheet(cycleId: number): void {
    this.loading.set(true);
    this.procurementService.getSheet(cycleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: s => {
          this.sheet.set(s);
          this.loading.set(false);
          s.items.forEach(i => {
            this.editValues[i.productId] = {
              vendorName: i.vendorName ?? '',
              vendorNotes: i.vendorNotes ?? '',
              procuredQty: i.procuredQty ?? 0,
              status: i.status
            };
          });
        },
        error: () => this.loading.set(false)
      });
  }

  onCycleChange(cycleId: number): void {
    this.selectedCycleId.set(cycleId);
    this.loadSheet(cycleId);
  }

  save(item: ProcurementItem): void {
    const val = this.editValues[item.productId];
    this.procurementService.update(this.selectedCycleId()!, item.id, val).subscribe({
      next: () => this.snackbar.open('Saved!', 'Close', { duration: 2000 })
    });
  }

  markAllProcured(): void {
    if (!this.selectedCycleId()) return;
    this.markingAll.set(true);
    this.procurementService.markAllProcured(this.selectedCycleId()!).subscribe({
      next: () => {
        this.markingAll.set(false);
        this.snackbar.open('All items marked as PROCURED. GOODS_LOADED triggered.', 'Close', { duration: 3000 });
        this.loadSheet(this.selectedCycleId()!);
      },
      error: () => this.markingAll.set(false)
    });
  }

  exportExcel(): void {
    this.exporting.set(true);
    this.procurementService.export(this.selectedCycleId()!).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `procurement-cycle-${this.selectedCycleId()}.xlsx`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => this.exporting.set(false)
    });
  }
}
