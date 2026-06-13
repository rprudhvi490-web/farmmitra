import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  form = new FormGroup({
    phoneNumber: new FormControl('', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]),
    referralCode: new FormControl('')
  });

  loading = signal(false);
  errorMsg = signal('');

  async register(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const phone = this.form.get('phoneNumber')!.value!;
    const referralCode = this.form.get('referralCode')!.value?.trim() ?? '';

    this.loading.set(true);
    this.errorMsg.set('');
    try {
      await this.authService.sendFirebaseOtp(phone, 'register');
      this.loading.set(false);
      this.router.navigate(['/auth/verify'], {
        state: { phone, referralCode, mode: 'register' }
      });
    } catch (err: any) {
      this.loading.set(false);
      if (err.status === 409) {
        this.errorMsg.set('This number is already registered. Please login.');
      } else if (err.status === 429) {
        this.errorMsg.set('Daily OTP limit reached. Please try again tomorrow.');
      } else {
        this.errorMsg.set(err.error?.message ?? 'Failed to send OTP. Please try again.');
      }
    }
  }
}
