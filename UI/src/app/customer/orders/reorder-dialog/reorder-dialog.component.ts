import { Component, Inject, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { CustomerOrder, CycleService, ProductService, Product, WeeklyCycle } from '../../services/customer.services';

interface ItemStatus {
  productId: number;
  productName: string;
  quantity: number;
  unit: string;
  warning: string | null;  // null = ok to add
}

@Component({
  selector: 'app-reorder-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="title-icon">replay</mat-icon>
      Reorder — {{ data.order.orderNumber }}
    </h2>

    <mat-dialog-content>
      @if (loading()) {
        <div class="loading-row"><mat-spinner diameter="32" /></div>
      } @else if (cycleBlocked()) {
        <div class="cycle-blocked">
          <mat-icon>schedule</mat-icon>
          <div>
            <strong>Ordering is currently closed.</strong>
            <p>The ordering cycle is not open right now. You can reorder when the next cycle opens (Monday).</p>
          </div>
        </div>
      } @else {
        <p class="hint">
          The following items will be <strong>appended</strong> to your current cart.
          Existing cart items will not be removed.
        </p>
        <div class="items-list">
          @for (item of itemStatuses(); track item.productId) {
            <div class="item-row" [class.item-blocked]="item.warning !== null">
              <div class="item-main">
                <span class="item-name">{{ item.productName }}</span>
                <span class="item-qty">{{ item.quantity }} {{ item.unit }}</span>
              </div>
              @if (item.warning) {
                <div class="item-warning">
                  <mat-icon>warning</mat-icon>
                  <span>{{ item.warning }}</span>
                </div>
              }
            </div>
          }
        </div>
        @if (addableCount() === 0) {
          <div class="no-items-note">
            <mat-icon>info</mat-icon>
            None of the items can be added to cart right now.
          </div>
        } @else if (addableCount() < itemStatuses().length) {
          <div class="partial-note">
            <mat-icon>info</mat-icon>
            {{ addableCount() }} of {{ itemStatuses().length }} items will be added. Items with warnings will be skipped.
          </div>
        }
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      @if (!loading() && !cycleBlocked() && addableCount() > 0) {
        <button mat-flat-button class="confirm-btn" (click)="confirm()">
          <mat-icon>add_shopping_cart</mat-icon> Add {{ addableCount() }} Item(s) to Cart
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-family: var(--fm-font);
      font-size: 18px;
      font-weight: 800;
      color: var(--fm-text);
      margin: 0;
    }

    .title-icon { color: var(--fm-accent); font-size: 22px; width: 22px; height: 22px; }

    .loading-row { display: flex; justify-content: center; padding: 24px 0; }

    .cycle-blocked {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      background: #fff8e1;
      border: 1px solid #ffe082;
      border-radius: 6px;
      padding: 14px;
      mat-icon { color: #f57f17; flex-shrink: 0; margin-top: 2px; }
      strong { font-family: var(--fm-font); font-size: 14px; color: var(--fm-text); }
      p { font-size: 13px; font-family: var(--fm-font); color: var(--fm-text-secondary); margin: 4px 0 0; }
    }

    .hint {
      font-size: 13px;
      font-family: var(--fm-font);
      color: var(--fm-text-secondary);
      margin: 0 0 16px;
      padding: 10px 12px;
      background: var(--fm-orange-50);
      border-left: 3px solid var(--fm-accent);
      border-radius: 4px;
    }

    .items-list {
      display: flex;
      flex-direction: column;
      max-height: 280px;
      overflow-y: auto;
    }

    .item-row {
      padding: 9px 0;
      border-bottom: 1px solid var(--fm-divider);
      &.item-blocked { opacity: 0.6; }
    }

    .item-main {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-family: var(--fm-font);
      .item-name { font-size: 14px; font-weight: 500; color: var(--fm-text); flex: 1; }
      .item-qty  { font-size: 13px; font-weight: 700; color: var(--fm-primary); min-width: 80px; text-align: right; }
    }

    .item-warning {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 4px;
      font-size: 11px;
      font-family: var(--fm-font);
      color: #e65100;
      mat-icon { font-size: 14px; width: 14px; height: 14px; color: #e65100; }
    }

    .no-items-note, .partial-note {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-top: 12px;
      font-size: 12px;
      font-family: var(--fm-font);
      color: var(--fm-text-secondary);
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    .confirm-btn {
      background: var(--fm-primary) !important;
      color: white !important;
      font-weight: 700;
      font-family: var(--fm-font);
    }
  `]
})
export class ReorderDialogComponent implements OnInit {
  private cycleService   = inject(CycleService);
  private productService = inject(ProductService);

  loading      = signal(true);
  cycleBlocked = signal(false);
  itemStatuses = signal<ItemStatus[]>([]);

  addableCount = () => this.itemStatuses().filter(i => i.warning === null).length;

  constructor(
    public dialogRef: MatDialogRef<ReorderDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { order: CustomerOrder }
  ) {}

  ngOnInit(): void {
    forkJoin({
      cycle:    this.cycleService.getCurrent(),
      products: this.productService.getAll()
    }).subscribe({
      next: ({ cycle, products }) => {
        if (!cycle || cycle.status !== 'OPEN') {
          this.cycleBlocked.set(true);
          this.loading.set(false);
          return;
        }
        const productMap = new Map<number, Product>(products.map(p => [p.id, p]));
        const statuses: ItemStatus[] = this.data.order.items.map(item => {
          const p = productMap.get(item.productId);
          let warning: string | null = null;
          if (!p || !p.available) {
            warning = 'Product no longer available';
          } else if (!p.stockConfigured) {
            warning = 'Stock limit not configured for this item yet — please check back shortly';
          } else if (p.soldOut) {
            warning = 'Stock Reached Limit — kindly try again later';
          }
          return { productId: item.productId, productName: item.productName, quantity: item.quantity, unit: item.unit, warning };
        });
        this.itemStatuses.set(statuses);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  confirm(): void { this.dialogRef.close(this.itemStatuses().filter(i => i.warning === null)); }
  cancel(): void  { this.dialogRef.close(null); }
}
