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
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminCycleService, WeeklyCycle } from '../services/admin.services';

@Component({
  selector: 'app-cycles',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule
  ],
  templateUrl: './cycles.component.html',
  styleUrl: './cycles.component.scss'
})
export class CyclesComponent implements OnInit {
  private cycleService = inject(AdminCycleService);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  cycles = signal<WeeklyCycle[]>([]);
  loading = signal(true);
  showCreateForm = signal(false);
  saving = signal(false);

  columns = ['cycleLabel', 'status', 'orderOpenAt', 'orderCloseAt', 'actions'];

  statusOptions = ['OPEN', 'CLOSED', 'PROCUREMENT', 'DELIVERING', 'COMPLETED'];

  createForm = new FormGroup({
    cycleLabel:      new FormControl('', Validators.required),
    orderOpenAt:     new FormControl('', Validators.required),
    orderCloseAt:    new FormControl('', Validators.required),
    deliveryDateSat: new FormControl('', Validators.required),
    deliveryDateSun: new FormControl('', Validators.required),
  });

  ngOnInit(): void {
    this.loadCycles();
  }

  loadCycles(): void {
    this.cycleService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: c => { this.cycles.set(c); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  create(): void {
    if (this.createForm.invalid) { this.createForm.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.createForm.value;
    const req = {
      cycleLabel:      v.cycleLabel,
      orderOpenAt:     v.orderOpenAt + ':00',
      orderCloseAt:    v.orderCloseAt + ':00',
      deliveryDateSat: v.deliveryDateSat,
      deliveryDateSun: v.deliveryDateSun
    };
    this.cycleService.create(req).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCreateForm.set(false);
        this.createForm.reset();
        this.snackbar.open('Cycle created!', 'Close', { duration: 3000 });
        this.loadCycles();
      },
      error: () => this.saving.set(false)
    });
  }

  open(id: number): void {
    this.cycleService.open(id).subscribe({
      next: () => { this.snackbar.open('Cycle opened!', 'Close', { duration: 2000 }); this.loadCycles(); }
    });
  }

  close(id: number): void {
    this.cycleService.close(id).subscribe({
      next: () => { this.snackbar.open('Cycle closed!', 'Close', { duration: 2000 }); this.loadCycles(); }
    });
  }

  updateStatus(id: number, status: string): void {
    this.cycleService.updateStatus(id, status).subscribe({
      next: () => { this.snackbar.open('Status updated!', 'Close', { duration: 2000 }); this.loadCycles(); }
    });
  }

  getStatusClass(status: string): string {
    return 'status-' + status.toLowerCase();
  }
}
