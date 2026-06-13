import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';
import { Auth, RecaptchaVerifier, signInWithPhoneNumber, ConfirmationResult } from '@angular/fire/auth';
import { firstValueFrom } from 'rxjs';

export interface SendOtpRequest {
  phoneNumber: string;
}

export interface VerifyOtpRequest {
  phoneNumber: string;
  otp: string;
  referralCode?: string;
}

export interface AuthResponse {
  token: string;
  expiresIn: number;
  phoneNumber: string;
  username: string;
  roles: string[];
  isNewUser: boolean;
  hasPassword: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private router = inject(Router);
  private firebaseAuth = inject(Auth);

  private readonly base = environment.apiBaseUrl;
  private confirmationResult: ConfirmationResult | null = null;
  private recaptchaVerifier: RecaptchaVerifier | null = null;

  

  verifyOtp(req: VerifyOtpRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/auth/verify-otp`, req).pipe(
      tap(res => { this.tokenService.save(res.token); this.tokenService.saveUsername(res.username); })
    );
  }

  logout(): void {
    const token = this.tokenService.get();
    this.tokenService.remove();
    // Fire and forget — backend invalidates token, we don't wait
    if (token) {
      this.http.post(`${this.base}/auth/logout`, {}).subscribe();
    }
    this.router.navigate(['/auth/login']);
  }

  redirectAfterLogin(isNewUser: boolean): void {
    if (this.tokenService.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
    } else if (this.tokenService.isDelivery()) {
      this.router.navigate(['/admin/batches']);
    } else if (this.tokenService.isProcurement()) {
      this.router.navigate(['/admin/procurement']);
    } else {
      this.router.navigate([isNewUser ? '/customer/profile' : '/customer/home']);
    }
  }

  async sendFirebaseOtp(phoneNumber: string): Promise<void> {
  if (!this.recaptchaVerifier) {
    this.recaptchaVerifier = new RecaptchaVerifier(this.firebaseAuth, 'recaptcha-container', {
      size: 'invisible'
    });
  }
  const e164 = '+91' + phoneNumber;
  this.confirmationResult = await signInWithPhoneNumber(this.firebaseAuth, e164, this.recaptchaVerifier);
}

// ADD: verify OTP with Firebase, then exchange for app JWT
async verifyFirebaseOtp(otp: string, referralCode?: string): Promise<AuthResponse> {
  if (!this.confirmationResult) throw new Error('No OTP sent yet');
  const credential = await this.confirmationResult.confirm(otp);
  const firebaseToken = await credential.user.getIdToken();

  return firstValueFrom(
    this.http.post<AuthResponse>(`${this.base}/auth/firebase-login`, {
      token: firebaseToken,
      referralCode: referralCode ?? null
    }).pipe(
      tap(res => {
        this.tokenService.save(res.token);
        this.tokenService.saveUsername(res.username);
      })
    )
  );
}
}
