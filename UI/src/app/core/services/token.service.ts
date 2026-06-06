import { Injectable } from '@angular/core';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;       // phone number is stored as subject
  roles: string[];
  exp: number;
  iat: number;
}

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly KEY = 'wb_token';

  private readonly NAME_KEY = 'wb_username';

  save(token: string): void {
    localStorage.setItem(this.KEY, token);
  }

  saveUsername(username: string): void {
    localStorage.setItem(this.NAME_KEY, username);
  }

  getUsername(): string {
    return localStorage.getItem(this.NAME_KEY) || 'Guest';
  }

  get(): string | null {
    return localStorage.getItem(this.KEY);
  }

  remove(): void {
    localStorage.removeItem(this.KEY);
    localStorage.removeItem(this.NAME_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.get() && !this.isExpired();
  }

  isExpired(): boolean {
    const token = this.get();
    if (!token) return true;
    try {
      const { exp } = jwtDecode<JwtPayload>(token);
      return Date.now() >= exp * 1000;
    } catch {
      return true;
    }
  }

  getRoles(): string[] {
    const token = this.get();
    if (!token) return [];
    try {
      return jwtDecode<JwtPayload>(token).roles ?? [];
    } catch {
      return [];
    }
  }

  getPhone(): string {
    const token = this.get();
    if (!token) return '';
    try {
      return jwtDecode<JwtPayload>(token).sub ?? '';
    } catch {
      return '';
    }
  }

  hasRole(role: string): boolean {
    return this.getRoles().includes(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN') || this.hasRole('ROLE_SUPER_ADMIN');
  }

  isCustomer(): boolean {
    return this.hasRole('ROLE_CUSTOMER');
  }

  isDelivery(): boolean {
    return this.hasRole('ROLE_DELIVERY');
  }

  isProcurement(): boolean {
    return this.hasRole('ROLE_PROCUREMENT') && !this.isAdmin();
  }
}
