import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminUserService, AdminUser, UserSession } from '../services/admin.services';
import { ToastService } from '../../core/services/toast.service';
import { Component as NgComponent, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

// ── Sessions Dialog ───────────────────────────────────────────
@NgComponent({
  selector: 'app-sessions-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Active Sessions — {{ data.user.username || data.user.phoneNumber }}</h2>
    <mat-dialog-content>
      @if (data.sessions.length === 0) {
        <p class="no-sessions">No active sessions found.</p>
      } @else {
        @for (s of data.sessions; track s.issuedAt) {
          <div class="session-row">
            <div class="session-info">
              <span class="device">{{ s.deviceHint || 'Unknown device' }}</span>
              <span class="last-used">Last used: {{ s.lastUsedAt | date:'dd MMM, hh:mm a' }}</span>
              <span class="expires">Expires: {{ s.expiredAt | date:'dd MMM yyyy' }}</span>
            </div>
            @if (s.activeNow) {
              <span class="active-badge">● Active Now</span>
            }
          </div>
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="close()">Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .no-sessions { color: #9e9e9e; font-family: var(--fm-font); padding: 8px 0; }
    .session-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: 12px 0; border-bottom: 1px solid var(--fm-divider);
    }
    .session-info { display: flex; flex-direction: column; gap: 2px; }
    .device { font-weight: 700; font-size: 13px; font-family: var(--fm-font); }
    .last-used, .expires { font-size: 12px; color: var(--fm-text-secondary); font-family: var(--fm-font); }
    .active-badge {
      color: #2e7d32; font-weight: 700; font-size: 12px;
      background: #e8f5e9; padding: 3px 10px; border-radius: 12px;
      font-family: var(--fm-font); white-space: nowrap;
    }
  `]
})
export class SessionsDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SessionsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: AdminUser; sessions: UserSession[] }
  ) {}
  close(): void { this.dialogRef.close(); }
}

// ── Users Component ───────────────────────────────────────────
@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatChipsModule, MatProgressSpinnerModule,
    MatDialogModule, MatTooltipModule
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  private userService = inject(AdminUserService);
  private toast       = inject(ToastService);
  private dialog      = inject(MatDialog);
  private destroyRef  = inject(DestroyRef);

  allUsers  = signal<AdminUser[]>([]);
  filtered  = signal<AdminUser[]>([]);
  loading   = signal(true);

  search = new FormControl('');
  columns = ['phone', 'username', 'roles', 'status', 'sessions', 'actions'];

  roleOptions = ['ROLE_CUSTOMER', 'ROLE_DELIVERY', 'ROLE_PROCUREMENT', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN'];

  // Track selected role per user for the assign dropdown
  selectedRole: Record<number, string> = {};

  ngOnInit(): void {
    this.load();
    this.search.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(q => {
        const query = (q ?? '').toLowerCase();
        this.filtered.set(
          this.allUsers().filter(u =>
            u.phoneNumber.includes(query) || u.username?.toLowerCase().includes(query)
          )
        );
      });
  }

  load(): void {
    this.userService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: u => { this.allUsers.set(u); this.filtered.set(u); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  block(id: number): void {
    this.userService.block(id).subscribe({
      next: () => { this.toast.success('User blocked.'); this.load(); }
    });
  }

  unblock(id: number): void {
    this.userService.unblock(id).subscribe({
      next: () => { this.toast.success('User unblocked.'); this.load(); }
    });
  }

  assignRole(userId: number): void {
    const roleId = this.selectedRole[userId];
    if (!roleId) return;
    this.userService.assignRole(userId, roleId).subscribe({
      next: () => {
        this.toast.success(`${roleId.replace('ROLE_', '')} role assigned.`);
        this.selectedRole[userId] = '';
        this.load();
      },
      error: (err: any) => this.toast.error(err.error?.message ?? 'Failed to assign role.')
    });
  }

  removeRole(userId: number, roleId: string): void {
    this.userService.removeRole(userId, roleId).subscribe({
      next: () => { this.toast.success(`${roleId.replace('ROLE_', '')} role removed.`); this.load(); },
      error: (err: any) => this.toast.error(err.error?.message ?? 'Failed to remove role.')
    });
  }

  viewSessions(user: AdminUser): void {
    this.userService.getSessions(user.userId).subscribe({
      next: sessions => {
        this.dialog.open(SessionsDialogComponent, {
          data: { user, sessions },
          width: '500px',
          maxWidth: '95vw'
        });
      },
      error: () => this.toast.error('Failed to load sessions.')
    });
  }
}
