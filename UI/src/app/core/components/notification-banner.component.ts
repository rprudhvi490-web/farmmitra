import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ToastService, Toast } from '../services/toast.service';

@Component({
  selector: 'app-notification-banner',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  template: `
    <div class="banner-stack">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="banner-item banner-{{ toast.type }}">
          <mat-icon class="banner-icon">{{ iconFor(toast.type) }}</mat-icon>
          <span class="banner-message">{{ toast.message }}</span>
          <button class="close-btn" (click)="toastService.dismiss(toast.id)" aria-label="Dismiss">
            <mat-icon>close</mat-icon>
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .banner-stack {
      position: sticky;
      top: 0;
      z-index: 200;
      display: flex;
      flex-direction: column;
      gap: 0;
    }

    .banner-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      font-family: var(--fm-font);
      font-size: 14px;
      font-weight: 600;
      animation: slideDown 0.2s ease-out;

      .banner-message { flex: 1; line-height: 1.4; }

      .banner-icon { font-size: 20px; width: 20px; height: 20px; flex-shrink: 0; }

      .close-btn {
        background: none;
        border: none;
        cursor: pointer;
        padding: 0;
        display: flex;
        align-items: center;
        opacity: 0.7;
        flex-shrink: 0;
        &:hover { opacity: 1; }
        mat-icon { font-size: 18px; width: 18px; height: 18px; }
      }
    }

    .banner-success {
      background: #e8f5e9;
      color: #1b5e20;
      border-bottom: 2px solid #a5d6a7;
      .banner-icon, .close-btn { color: #2e7d32; }
    }

    .banner-error {
      background: #ffebee;
      color: #b71c1c;
      border-bottom: 2px solid #ef9a9a;
      .banner-icon, .close-btn { color: #c62828; }
    }

    .banner-info {
      background: #e3f2fd;
      color: #0d47a1;
      border-bottom: 2px solid #90caf9;
      .banner-icon, .close-btn { color: #1565c0; }
    }

    .banner-warning {
      background: #fff8e1;
      color: #e65100;
      border-bottom: 2px solid #ffe082;
      .banner-icon, .close-btn { color: #f57f17; }
    }

    @keyframes slideDown {
      from { transform: translateY(-100%); opacity: 0; }
      to   { transform: translateY(0);     opacity: 1; }
    }
  `]
})
export class NotificationBannerComponent {
  toastService = inject(ToastService);

  iconFor(type: string): string {
    const map: Record<string, string> = {
      success: 'check_circle',
      error:   'error',
      info:    'info',
      warning: 'warning'
    };
    return map[type] ?? 'notifications';
  }
}
