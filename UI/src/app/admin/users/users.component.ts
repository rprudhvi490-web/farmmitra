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
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminUserService, AdminUser } from '../services/admin.services';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatChipsModule, MatProgressSpinnerModule
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  private userService = inject(AdminUserService);
  private snackbar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);

  allUsers = signal<AdminUser[]>([]);
  filtered = signal<AdminUser[]>([]);
  loading = signal(true);

  search = new FormControl('');
  columns = ['phone', 'username', 'roles', 'status', 'actions'];

  roleOptions = ['ROLE_CUSTOMER', 'ROLE_DELIVERY', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN'];

  ngOnInit(): void {
    this.load();
    this.search.valueChanges.subscribe(q => {
      const query = (q ?? '').toLowerCase();
      this.filtered.set(
        this.allUsers().filter(u =>
          u.phoneNumber.includes(query) || u.username?.toLowerCase().includes(query)
        )
      );
    });
  }

  load(): void {
    this.userService.getAll().subscribe({
      next: u => { this.allUsers.set(u); this.filtered.set(u); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  block(id: number): void {
    this.userService.block(id).subscribe({
      next: () => { this.snackbar.open('User blocked', 'Close', { duration: 2000 }); this.load(); }
    });
  }

  unblock(id: number): void {
    this.userService.unblock(id).subscribe({
      next: () => { this.snackbar.open('User unblocked', 'Close', { duration: 2000 }); this.load(); }
    });
  }

  assignRole(userId: number, roleId: string): void {
    this.userService.assignRole(userId, roleId).subscribe({
      next: () => { this.snackbar.open(`${roleId} assigned`, 'Close', { duration: 2000 }); this.load(); },
      error: (err) => this.snackbar.open(err.error?.message ?? 'Role already assigned', 'Close', { duration: 3000 })
    });
  }
}
