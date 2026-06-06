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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CloudinaryService } from '../../core/services/cloudinary.service';
import { ToastService } from '../../core/services/toast.service';

export interface Category {
  id: number;
  name: string;
  imageUrl: string;
  displayOrder: number;
  active: boolean;
}

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.scss'
})
export class CategoriesComponent implements OnInit {
  private http = inject(HttpClient);
  private cloudinary = inject(CloudinaryService);
  private toast = inject(ToastService);
  private destroyRef = inject(DestroyRef);
  private base = environment.apiBaseUrl;

  categories = signal<Category[]>([]);
  loading = signal(true);
  saving = signal(false);
  uploading = signal(false);
  showForm = signal(false);
  editingId = signal<number | null>(null);

  columns = ['image', 'name', 'displayOrder', 'active', 'actions'];

  form = new FormGroup({
    name:         new FormControl('', Validators.required),
    imageUrl:     new FormControl(''),
    displayOrder: new FormControl(1, Validators.required),
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.http.get<Category[]>(`${this.base}/categories`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: c => { this.categories.set(c); this.loading.set(false); }, error: () => this.loading.set(false) });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', imageUrl: '', displayOrder: this.categories().length + 1 });
    this.showForm.set(true);
  }

  openEdit(cat: Category): void {
    this.editingId.set(cat.id);
    this.form.setValue({ name: cat.name, imageUrl: cat.imageUrl ?? '', displayOrder: cat.displayOrder });
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
        this.toast.success('Image uploaded!');
      },
      error: () => { this.uploading.set(false); this.toast.error('Image upload failed.'); }
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const req = this.form.value;
    const call = this.editingId()
      ? this.http.put(`${this.base}/categories/${this.editingId()}`, req)
      : this.http.post(`${this.base}/categories`, req);
    call.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.toast.success(this.editingId() ? 'Category updated!' : 'Category created!');
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  cancel(): void { this.showForm.set(false); }
}
