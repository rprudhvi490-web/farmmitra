import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NotificationService, AppNotification } from '../services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent implements OnInit {
  private notifService = inject(NotificationService);
  private destroyRef = inject(DestroyRef);

  notifications = signal<AppNotification[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.notifService.getMy()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: n => { this.notifications.set(n ?? []); this.loading.set(false); },
        error: () => { this.notifications.set([]); this.loading.set(false); }
      });
  }

  markRead(id: number): void {
    this.notifService.markRead(id).subscribe(() => {
      this.notifications.update(list =>
        list.map(n => n.id === id ? { ...n, readStatus: true } : n)
      );
    });
  }

  markAllRead(): void {
    this.notifService.markAllRead().subscribe(() => {
      this.notifications.update(list => list.map(n => ({ ...n, readStatus: true })));
    });
  }

  unreadCount(): number {
    return this.notifications().filter(n => !n.readStatus).length;
  }
}
