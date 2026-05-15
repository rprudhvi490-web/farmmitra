import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';

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

  private readonly base = environment.apiBaseUrl;

  sendOtp(phoneNumber: string): Observable<{ message: string; phoneNumber: string }> {
    return this.http.post<{ message: string; phoneNumber: string }>(
      `${this.base}/auth/send-otp`,
      { phoneNumber }
    );
  }

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
    } else {
      // Customer — if new user go to profile, else home
      this.router.navigate([isNewUser ? '/customer/profile' : '/customer/home']);
    }
  }
}
