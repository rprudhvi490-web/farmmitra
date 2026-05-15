import { Component, inject, signal, OnInit, OnDestroy, DestroyRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CycleService, WeeklyCycle } from '../services/customer.services';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit, OnDestroy {
  private cycleService = inject(CycleService);
  private destroyRef = inject(DestroyRef);

  cycle = signal<WeeklyCycle | null>(null);
  loading = signal(true);
  countdown = signal('');

  private countdownInterval: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.cycleService.getCurrent()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cycle) => {
          this.cycle.set(cycle);
          this.loading.set(false);
          if (cycle?.timeRemainingSeconds) {
            this.startCountdown(cycle.timeRemainingSeconds);
          }
        },
        error: () => this.loading.set(false)
      });
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
  }

  private startCountdown(seconds: number): void {
    let remaining = seconds;
    this.updateCountdownDisplay(remaining);
    this.countdownInterval = setInterval(() => {
      remaining--;
      if (remaining <= 0) {
        clearInterval(this.countdownInterval!);
        this.countdown.set('Ordering closed');
        return;
      }
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
      OPEN:        { icon: 'shopping_basket', color: '#2e7d32', message: 'Ordering is open! Order by Wednesday 2 PM' },
      CLOSED:      { icon: 'schedule',        color: '#e65100', message: 'Ordering closed. Procurement in progress.' },
      PROCUREMENT: { icon: 'agriculture',     color: '#f57f17', message: "We're buying your groceries in Kadapa!" },
      DELIVERING:  { icon: 'local_shipping',  color: '#1565c0', message: 'Your order is on the way!' },
      COMPLETED:   { icon: 'check_circle',    color: '#4a148c', message: 'Delivered! Next cycle opens Monday.' }
    };
    return map[status] ?? { icon: 'info', color: '#757575', message: 'Next ordering window opens Monday.' };
  }
}
