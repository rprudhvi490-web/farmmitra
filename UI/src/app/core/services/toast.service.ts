import { Injectable, signal } from '@angular/core';
import { environment } from '../../../environments/environment';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  autoClose: boolean;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 0;
  readonly toasts = signal<Toast[]>([]);

  private push(message: string, type: ToastType, autoClose: boolean): void {
    const id = ++this.nextId;
    this.toasts.update(t => [...t, { id, message, type, autoClose }]);
    if (autoClose) {
      setTimeout(() => this.dismiss(id), environment.notificationDurationMs);
    }
  }

  success(message: string): void { this.push(message, 'success', true); }
  info(message: string):    void { this.push(message, 'info',    true); }
  warning(message: string): void { this.push(message, 'warning', true); }
  error(message: string): void {
    // Replace any existing error toasts — no pile-up
    this.toasts.update(t => t.filter(n => n.type !== 'error'));
    const id = ++this.nextId;
    this.toasts.update(t => [...t, { id, message, type: 'error', autoClose: true }]);
    setTimeout(() => this.dismiss(id), environment.notificationErrorDurationMs);
  }

  dismiss(id: number): void {
    this.toasts.update(t => t.filter(n => n.id !== id));
  }

  clear(): void { this.toasts.set([]); }
}
