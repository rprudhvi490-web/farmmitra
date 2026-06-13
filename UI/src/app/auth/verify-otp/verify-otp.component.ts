import { Component, inject, signal, OnInit, OnDestroy, ViewChildren, QueryList, ElementRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormArray, FormControl, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatProgressSpinnerModule, RouterLink
  ],
  templateUrl: './verify-otp.component.html',
  styleUrl: './verify-otp.component.scss'
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);
  private snackbar = inject(MatSnackBar);

  @ViewChildren('otpBox') otpBoxes!: QueryList<ElementRef<HTMLInputElement>>;

  phone = signal('');
  referralCode = signal('');
  mode = signal<'login' | 'forgot-password' | 'register'>('login');
  loading = signal(false);
  resendCooldown = signal(0);

  digits = new FormArray(
    Array.from({ length: 6 }, () => new FormControl('', [Validators.required, Validators.pattern(/^\d$/)]))
  );

  private cooldownInterval: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state ?? history.state;
    const phone = state?.['phone'] ?? '';
    const referral = state?.['referralCode'] ?? '';
    const mode = state?.['mode'] ?? 'login';

    if (!phone) {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.phone.set(phone);
    this.referralCode.set(referral);
    this.mode.set(mode);
    this.startCooldown();
  }

  ngOnDestroy(): void {
    if (this.cooldownInterval) clearInterval(this.cooldownInterval);
  }

  onDigitInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '').slice(-1);
    this.digits.at(index).setValue(value);
    if (value && index < 5) {
      this.otpBoxes.toArray()[index + 1].nativeElement.focus();
    }
    if (this.digits.value.every(d => d !== '')) {
      this.verify();
    }
  }

  onDigitKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      const current = this.digits.at(index).value;
      if (!current && index > 0) {
        this.digits.at(index - 1).setValue('');
        this.otpBoxes.toArray()[index - 1].nativeElement.focus();
      }
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) ?? '';
    pasted.split('').forEach((char, i) => { if (i < 6) this.digits.at(i).setValue(char); });
    const lastIndex = Math.min(pasted.length - 1, 5);
    this.otpBoxes.toArray()[lastIndex]?.nativeElement.focus();
    if (pasted.length === 6) this.verify();
  }

  async verify(): Promise<void> {
    const otp = this.digits.value.join('');
    if (otp.length !== 6) return;
    this.loading.set(true);
    try {
      const res = await this.authService.verifyFirebaseOtp(otp, this.referralCode() || undefined);
      this.loading.set(false);
      if (this.mode() === 'forgot-password') {
        this.router.navigate(['/customer/profile'], { state: { forceSetPassword: true } });
      } else {
        this.authService.redirectAfterLogin(res.isNewUser);
      }
    } catch (err: any) {
      this.loading.set(false);
      this.digits.controls.forEach(c => c.setValue(''));
      this.otpBoxes.toArray()[0]?.nativeElement.focus();
      this.snackbar.open('Invalid OTP. Please try again.', 'Close', { duration: 3000 });
    }
  }

  async resendOtp(): Promise<void> {
    if (this.resendCooldown() > 0) return;
    try {
      await this.authService.sendFirebaseOtp(this.phone(), this.mode());
      this.startCooldown();
    } catch (err: any) {
      this.snackbar.open(err.error?.message ?? 'Failed to resend OTP. Please try again.', 'Close', { duration: 3000 });
    }
  }

  private startCooldown(): void {
    this.resendCooldown.set(60);
    this.cooldownInterval = setInterval(() => {
      this.resendCooldown.update(v => {
        if (v <= 1) { clearInterval(this.cooldownInterval!); return 0; }
        return v - 1;
      });
    }, 1000);
  }
}
