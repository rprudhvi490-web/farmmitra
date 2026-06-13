import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressSpinnerModule
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  phone = new FormControl('', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]);
  loading = signal(false);
  errorMsg = signal('');

  async sendOtp(): Promise<void> {
    if (this.phone.invalid) { this.phone.markAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');
    try {
      await this.authService.sendFirebaseOtp(this.phone.value!, 'forgot-password');
      this.loading.set(false);
      this.router.navigate(['/auth/verify'], {
        state: { phone: this.phone.value, mode: 'forgot-password' }
      });
    } catch (err: any) {
      this.loading.set(false);
      if (err.status === 429) {
        this.errorMsg.set('Daily OTP limit reached. Please try again tomorrow.');
      } else if (err.status === 404) {
        this.errorMsg.set('No account found with this number. Please register first.');
      } else {
        this.errorMsg.set('Failed to send OTP. Please try again.');
      }
    }
  }
}
