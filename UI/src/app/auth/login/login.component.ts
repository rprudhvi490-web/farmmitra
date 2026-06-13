import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatProgressSpinnerModule, MatTabsModule, MatIconModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  // OTP tab
  phone = new FormControl('', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]);
  loading = signal(false);

  // Password tab
  passwordForm = new FormGroup({
    phoneNumber: new FormControl('', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]),
    password:    new FormControl('', [Validators.required, Validators.minLength(6)])
  });
  passwordLoading = signal(false);
  showPassword = signal(false);

  async sendOtp(): Promise<void> {

  if (this.phone.invalid) { this.phone.markAsTouched(); return; }
  this.loading.set(true);
  try {
    const rawNumber = this.phone.value; // e.g., "9876543210"
  
    await this.authService.sendFirebaseOtp(rawNumber!);
    this.loading.set(false);
    this.router.navigate(['/auth/verify'], { state: { phone: this.phone.value } });
  } catch (err: any) {
    this.loading.set(false);
    console.error('Firebase OTP error', err);
  }
}

  loginWithPassword(): void {
    if (this.passwordForm.invalid) { this.passwordForm.markAllAsTouched(); return; }
    this.passwordLoading.set(true);
    this.http.post<any>(`${this.base}/auth/login`, this.passwordForm.value).subscribe({
      next: (res) => {
        this.tokenService.save(res.token);
        this.tokenService.saveUsername(res.username);
        this.passwordLoading.set(false);
        this.authService.redirectAfterLogin(false);
      },
      error: () => this.passwordLoading.set(false)
    });
  }
}
