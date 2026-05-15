import { Component, inject, signal, OnInit, OnDestroy, ViewChildren, QueryList, ElementRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormArray, FormControl, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    RouterLink
  ],
  templateUrl: './verify-otp.component.html',
  styleUrl: './verify-otp.component.scss'
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);

  @ViewChildren('otpBox') otpBoxes!: QueryList<ElementRef<HTMLInputElement>>;

  phone = signal('');
  loading = signal(false);
  resendCooldown = signal(0);

  // 6 individual single-digit controls
  digits = new FormArray(
    Array.from({ length: 6 }, () => new FormControl('', [Validators.required, Validators.pattern(/^\d$/)]))
  );

  private cooldownInterval: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    // Phone passed via router state from login screen
    const nav = this.router.getCurrentNavigation();
    const phone = nav?.extras?.state?.['phone']
      ?? history.state?.['phone']
      ?? '';

    if (!phone) {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.phone.set(phone);
    this.startCooldown();
  }

  ngOnDestroy(): void {
    if (this.cooldownInterval) clearInterval(this.cooldownInterval);
  }

  // Called on each digit box keyup
  onDigitInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '').slice(-1); // only last digit
    this.digits.at(index).setValue(value);

    if (value && index < 5) {
      // Move focus to next box
      this.otpBoxes.toArray()[index + 1].nativeElement.focus();
    }

    // Auto-submit when all 6 filled
    if (this.digits.value.every(d => d !== '')) {
      this.verify();
    }
  }

  onDigitKeydown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      const current = this.digits.at(index).value;
      if (!current && index > 0) {
        // Move focus to previous box on backspace when empty
        this.digits.at(index - 1).setValue('');
        this.otpBoxes.toArray()[index - 1].nativeElement.focus();
      }
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) ?? '';
    pasted.split('').forEach((char, i) => {
      if (i < 6) this.digits.at(i).setValue(char);
    });
    // Focus last filled box
    const lastIndex = Math.min(pasted.length - 1, 5);
    this.otpBoxes.toArray()[lastIndex]?.nativeElement.focus();

    if (pasted.length === 6) this.verify();
  }

  verify(): void {
    const otp = this.digits.value.join('');
    if (otp.length !== 6) return;

    this.loading.set(true);

    this.authService.verifyOtp({ phoneNumber: this.phone(), otp }).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.authService.redirectAfterLogin(res.isNewUser);
      },
      error: () => {
        this.loading.set(false);
        // Clear OTP boxes on error so user can re-enter
        this.digits.controls.forEach(c => c.setValue(''));
        this.otpBoxes.toArray()[0]?.nativeElement.focus();
      }
    });
  }

  resendOtp(): void {
    if (this.resendCooldown() > 0) return;
    this.authService.sendOtp(this.phone()).subscribe({
      next: () => this.startCooldown()
    });
  }

  private startCooldown(): void {
    this.resendCooldown.set(60);
    this.cooldownInterval = setInterval(() => {
      this.resendCooldown.update(v => {
        if (v <= 1) {
          clearInterval(this.cooldownInterval!);
          return 0;
        }
        return v - 1;
      });
    }, 1000);
  }
}
