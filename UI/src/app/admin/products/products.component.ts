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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CloudinaryService } from '../../core/services/cloudinary.service';
import { Category } from '../categories/categories.component';

export interface Product {
  id: number;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  unit: string;
  pricePerUnit: number;
  imageUrl: string;
  available: boolean;
  minOrderQty: number;
}

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatProgressSpinnerModule
  ],
  templateUrl: './products.component.html',
  styleUrl: './products.component.scss'
})
export class AdminProductsComponent implements OnInit {
  private http = inject(HttpClient);
  private cloudinary = inject(CloudinaryService);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  private base = environment.apiBaseUrl;

  products = signal<Product[]>([]);
  categories = signal<Category[]>([]);
  loading = signal(true);
  saving = signal(false);
  uploading = signal(false);
  showForm = signal(false);
  editingId = signal<number | null>(null);

  columns = ['image', 'name', 'category', 'price', 'unit', 'available', 'actions'];

  form = new FormGroup({
    name:         new FormControl('', Validators.required),
    description:  new FormControl(''),
    categoryId:   new FormControl<number | null>(null, Validators.required),
    unit:         new FormControl('kg', Validators.required),
    pricePerUnit: new FormControl<number>(0, [Validators.required, Validators.min(0.01)]),
    minOrderQty:  new FormControl<number>(0.5, Validators.required),
    imageUrl:     new FormControl(''),
    available:    new FormControl(true),
  });

  ngOnInit(): void {
    this.http.get<Category[]>(`${this.base}/categories`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => this.categories.set(c));
    this.load();
  }

  load(): void {
    this.http.get<Product[]>(`${this.base}/products/all`)
      .subscribe({ next: p => { this.products.set(p); this.loading.set(false); }, error: () => this.loading.set(false) });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', description: '', categoryId: null, unit: 'kg', pricePerUnit: 0, minOrderQty: 0.5, imageUrl: '', available: true });
    this.showForm.set(true);
  }

  openEdit(p: Product): void {
    this.editingId.set(p.id);
    this.form.setValue({ name: p.name, description: p.description ?? '', categoryId: p.categoryId, unit: p.unit, pricePerUnit: p.pricePerUnit, minOrderQty: p.minOrderQty, imageUrl: p.imageUrl ?? '', available: p.available });
    this.showForm.set(true);
  }

  onImageSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploading.set(true);
    this.cloudinary.upload(file).subscribe({
      next: url => {
        this.form.patchValue({ imageUrl: url });
        this.uploading.set(false);
        this.snackbar.open('Image uploaded!', 'Close', { duration: 2000 });
      },
      error: () => { this.uploading.set(false); this.snackbar.open('Upload failed', 'Close', { duration: 3000 }); }
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const req = this.form.value;
    const call = this.editingId()
      ? this.http.put(`${this.base}/products/${this.editingId()}`, req)
      : this.http.post(`${this.base}/products`, req);
    call.subscribe({
      next: () => { this.saving.set(false); this.showForm.set(false); this.snackbar.open(this.editingId() ? 'Updated!' : 'Created!', 'Close', { duration: 2000 }); this.load(); },
      error: () => this.saving.set(false)
    });
  }

  toggleAvailability(p: Product): void {
    this.http.put(`${this.base}/products/${p.id}/availability`, { available: !p.available })
      .subscribe({ next: () => { this.snackbar.open(`${p.name} ${!p.available ? 'enabled' : 'disabled'}`, 'Close', { duration: 2000 }); this.load(); } });
  }

  cancel(): void { this.showForm.set(false); }
}
