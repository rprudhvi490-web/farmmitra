import { Component, inject, signal, computed, OnInit, OnDestroy, DestroyRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { CycleService, ProductService, Product, Category, WeeklyCycle } from '../services/customer.services';
import { CartService } from '../services/cart.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatFormFieldModule, MatInputModule
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit, OnDestroy {
  private cycleService = inject(CycleService);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private snackbar = inject(MatSnackBar);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  cycle = signal<WeeklyCycle | null>(null);
  categories = signal<Category[]>([]);
  products = signal<Product[]>([]);
  cycleLoading = signal(true);
  productsLoading = signal(true);
  selectedCategory = signal<number | null>(null);
  countdown = signal('');
  quantities = signal<Record<number, number>>({});

  searchControl = new FormControl('');

  private countdownInterval: ReturnType<typeof setInterval> | null = null;

  // Products grouped by category for display
  productsByCategory = computed(() => {
    const cat = this.selectedCategory();
    const search = (this.searchControl.value ?? '').toLowerCase();
    let filtered = this.products();
    if (cat !== null) filtered = filtered.filter(p => p.categoryId === cat);
    if (search) filtered = filtered.filter(p => p.name.toLowerCase().includes(search));
    // Group by category
    const map = new Map<string, Product[]>();
    filtered.forEach(p => {
      const key = p.categoryName;
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(p);
    });
    return map;
  });

  ngOnInit(): void {
    this.cycleService.getCurrent()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: cycle => {
          this.cycle.set(cycle);
          this.cycleLoading.set(false);
          if (cycle?.timeRemainingSeconds) this.startCountdown(cycle.timeRemainingSeconds);
        },
        error: () => this.cycleLoading.set(false)
      });

    this.productService.getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(c => this.categories.set(c));

    this.loadProducts();

    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.loadProducts());
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
  }

  loadProducts(): void {
    this.productsLoading.set(true);
    this.productService.getAll(undefined, this.searchControl.value ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => {
          // Sort by rating desc
          const sorted = [...p].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
          this.products.set(sorted);
          this.productsLoading.set(false);
          const q: Record<number, number> = {};
          sorted.forEach(prod => q[prod.id] = prod.minOrderQty);
          this.quantities.set(q);
        },
        error: () => this.productsLoading.set(false)
      });
  }

  selectCategory(id: number | null): void {
    this.selectedCategory.set(id);
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

  get categoryEntries(): [string, Product[]][] {
    return Array.from(this.productsByCategory().entries());
  }

  private startCountdown(seconds: number): void {
    let remaining = seconds;
    this.updateCountdownDisplay(remaining);
    this.countdownInterval = setInterval(() => {
      remaining--;
      if (remaining <= 0) { clearInterval(this.countdownInterval!); this.countdown.set('Ordering closed'); return; }
      this.updateCountdownDisplay(remaining);
    }, 1000);
  }

  private updateCountdownDisplay(seconds: number): void {
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (d > 0) this.countdown.set(`${d}d ${h}h ${m}m`);
    else if (h > 0) this.countdown.set(`${h}h ${m}m ${s}s`);
    else this.countdown.set(`${m}m ${s}s`);
  }

  getStatusConfig(status: string): { icon: string; color: string; message: string } {
    const map: Record<string, { icon: string; color: string; message: string }> = {
      OPEN:        { icon: 'shopping_basket', color: '#2e7d32', message: 'Ordering open — order by Wednesday 2 PM' },
      CLOSED:      { icon: 'schedule',        color: '#e65100', message: 'Ordering closed. Procurement in progress.' },
      PROCUREMENT: { icon: 'agriculture',     color: '#f57f17', message: "We're buying your groceries!" },
      DELIVERING:  { icon: 'local_shipping',  color: '#1565c0', message: 'Your order is on the way!' },
      COMPLETED:   { icon: 'check_circle',    color: '#4a148c', message: 'Delivered! Next cycle opens Monday.' }
    };
    return map[status] ?? { icon: 'info', color: '#757575', message: 'Next ordering window opens Monday.' };
  }
}
