import { Component, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { NotificationAdminService } from '../services/admin.services';

@Component({
  selector: 'app-admin-notifications',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatTabsModule
  ],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class AdminNotificationsComponent {
  private svc = inject(NotificationAdminService);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  broadcastSending = signal(false);
  directSending = signal(false);

  broadcast = { title: '', body: '', type: 'ANNOUNCEMENT' };
  direct = { userId: null as number | null, title: '', body: '', type: 'ORDER_UPDATE' };

  notificationTypes = ['ANNOUNCEMENT', 'ORDER_UPDATE', 'TRANSPORT_UPDATE', 'GENERAL'];

  sendBroadcast(): void {
    if (!this.broadcast.title || !this.broadcast.body) return;
    this.broadcastSending.set(true);
    this.svc.broadcast(this.broadcast)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackbar.open('Broadcast sent!', 'Close', { duration: 3000 });
          this.broadcast = { title: '', body: '', type: 'ANNOUNCEMENT' };
          this.broadcastSending.set(false);
        },
        error: () => this.broadcastSending.set(false)
      });
  }

  sendDirect(): void {
    if (!this.direct.userId || !this.direct.title || !this.direct.body) return;
    this.directSending.set(true);
    this.svc.sendToUser(this.direct)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackbar.open('Notification sent!', 'Close', { duration: 3000 });
          this.direct = { userId: null, title: '', body: '', type: 'ORDER_UPDATE' };
          this.directSending.set(false);
        },
        error: () => this.directSending.set(false)
      });
  }
}
