import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { TokenService } from '../../core/services/token.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatButtonModule,
            MatFormFieldModule, MatInputModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private http = inject(HttpClient);
  private snackbar = inject(MatSnackBar);
  private router = inject(Router);
  private tokenService = inject(TokenService);
  private destroyRef = inject(DestroyRef);
  private base = environment.apiBaseUrl;

  loading = signal(true);
  saving = signal(false);
  savingPassword = signal(false);
  hasPassword = signal(false);
  showPassword = signal(false);

  form = new FormGroup({
    username:   new FormControl(''),
    firstName:  new FormControl(''),
    lastName:   new FormControl(''),
    email:      new FormControl(''),
    flatNumber: new FormControl(''),
    block:      new FormControl('')
  });

  passwordForm = new FormGroup({
    password:        new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPassword: new FormControl('', [Validators.required])
  });

  ngOnInit(): void {
    this.http.get<any>(`${this.base}/users/me`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: u => {
          this.form.patchValue({
            username: u.username, firstName: u.firstName,
            lastName: u.lastName, email: u.email,
            flatNumber: u.flatNumber, block: u.block
          });
          this.hasPassword.set(u.hasPassword ?? false);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  save(): void {
    this.saving.set(true);
    this.http.put(`${this.base}/users/me`, this.form.value).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackbar.open('Profile updated!', 'Close', { duration: 2000 });
        this.router.navigate(['/customer/orders']);
      },
      error: () => this.saving.set(false)
    });
  }

  setPassword(): void {
    if (this.passwordForm.invalid) { this.passwordForm.markAllAsTouched(); return; }
    const { password, confirmPassword } = this.passwordForm.value;
    if (password !== confirmPassword) {
      this.snackbar.open('Passwords do not match', 'Close', { duration: 3000 });
      return;
    }
    this.savingPassword.set(true);
    this.http.put(`${this.base}/auth/set-password`, { password, confirmPassword }).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.hasPassword.set(true);
        this.passwordForm.reset();
        this.snackbar.open('Password set! You can now login with password.', 'Close', { duration: 4000 });
      },
      error: () => this.savingPassword.set(false)
    });
  }
}
