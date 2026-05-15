import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'verify',
    loadComponent: () => import('./verify-otp/verify-otp.component').then(m => m.VerifyOtpComponent)
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];
