import { Component, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { ProductService, Product, Category } from '../services/customer.services';
import { CartService } from '../services/cart.service';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './products.component.html',
  styleUrl: './products.component.scss'
})
export class ProductsComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private snackbar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  categories = signal<Category[]>([]);
  products = signal<Product[]>([]);
  loading = signal(true);
  selectedCategory = signal<number | null>(null);

  searchControl = new FormControl('');

  // quantity per product — track how many user wants to add
  quantities = signal<Record<number, number>>({});

  ngOnInit(): void {
    this.productService.getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => this.categories.set(c));

    this.loadProducts();

    // Search with debounce
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.loadProducts());
  }

  loadProducts(): void {
    this.loading.set(true);
    this.productService.getAll(
      this.selectedCategory() ?? undefined,
      this.searchControl.value ?? undefined
    ).pipe(takeUntilDestroyed(this.destroyRef))
     .subscribe({
       next: p => {
         this.products.set(p);
         this.loading.set(false);
         // init quantities to minOrderQty
         const q: Record<number, number> = {};
         p.forEach(prod => q[prod.id] = prod.minOrderQty);
         this.quantities.set(q);
       },
       error: () => this.loading.set(false)
     });
  }

  selectCategory(id: number | null): void {
    this.selectedCategory.set(id);
    this.loadProducts();
  }

  getQty(productId: number): number {
    return this.quantities()[productId] ?? 0;
  }

  adjustQty(productId: number, minQty: number, delta: number): void {
    const current = this.getQty(productId);
    const next = Math.max(minQty, +(current + delta).toFixed(1));
    this.quantities.update(q => ({ ...q, [productId]: next }));
  }

  addToCart(product: Product): void {
    const qty = this.getQty(product.id);
    this.cartService.addOrUpdate({
      productId: product.id,
      productName: product.name,
      unit: product.unit,
      pricePerUnit: product.pricePerUnit,
      quantity: qty,
      minOrderQty: product.minOrderQty
    });
    this.snackbar.open(`${product.name} added to cart`, 'View Cart', { duration: 2500 })
      .onAction().subscribe(() => this.router.navigate(['/customer/cart']));
  }

  isInCart(productId: number): boolean {
    return this.cartService.getItems().some(i => i.productId === productId);
  }
}
