import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TransportService, TransportStage, AdminCycleService, WeeklyCycle } from '../services/admin.services';

@Component({
  selector: 'app-transport',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatProgressSpinnerModule
  ],
  templateUrl: './transport.component.html',
  styleUrl: './transport.component.scss'
})
export class TransportComponent implements OnInit {
  private transportService = inject(TransportService);
  private cycleService = inject(AdminCycleService);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  cycles = signal<WeeklyCycle[]>([]);
  stages = signal<TransportStage[]>([]);
  selectedCycleId = signal<number | null>(null);
  loading = signal(false);
  saving = signal(false);

  readonly stageOrder = [
    'PROCUREMENT_STARTED', 'GOODS_LOADED', 'IN_TRANSIT',
    'ARRIVED', 'PACKING', 'DISPATCHED'
  ];

  stageOptions = [
    { code: 'PROCUREMENT_STARTED', label: 'Procurement Started' },
    { code: 'GOODS_LOADED',        label: 'Goods Loaded' },
    { code: 'IN_TRANSIT',          label: 'In Transit' },
    { code: 'ARRIVED',             label: 'Arrived in Hyderabad' },
    { code: 'PACKING',             label: 'Packing in Progress' },
    { code: 'DISPATCHED',          label: 'Out for Delivery' }
  ];

  // Returns only stages that come after the last recorded stage
  getAvailableStages() {
    const recorded = this.stages().map(s => s.stage);
    if (recorded.length === 0) return this.stageOptions;
    const lastStage = recorded[recorded.length - 1];
    const lastIndex = this.stageOrder.indexOf(lastStage);
    return this.stageOptions.filter(s => this.stageOrder.indexOf(s.code) > lastIndex);
  }

  form = new FormGroup({
    stage: new FormControl('', Validators.required),
    notes: new FormControl('')
  });

  ngOnInit(): void {
    this.cycleService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => {
        this.cycles.set(c);
        if (c.length > 0) { this.selectedCycleId.set(c[0].id); this.loadStages(c[0].id); }
      });
  }

  loadStages(cycleId: number): void {
    this.loading.set(true);
    this.transportService.getLog(cycleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: s => { this.stages.set(s); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  onCycleChange(cycleId: number): void {
    this.selectedCycleId.set(cycleId);
    this.loadStages(cycleId);
  }

  addStage(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.transportService.addStage(this.selectedCycleId()!, this.form.value).subscribe({
      next: () => {
        this.saving.set(false);
        this.form.reset();
        this.snackbar.open('Stage added! Customers notified.', 'Close', { duration: 3000 });
        this.loadStages(this.selectedCycleId()!);
      },
      error: () => this.saving.set(false)
    });
  }

  getStageLabel(code: string): string {
    return this.stageOptions.find(s => s.code === code)?.label ?? code;
  }
}
