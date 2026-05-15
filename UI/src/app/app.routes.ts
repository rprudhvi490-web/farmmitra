import { Routes } from '@angular/router';
import { authGuard } from './core/guards/guards';
import { roleGuard } from './core/guards/guards';
import { guestGuard } from './core/guards/guards';

export const routes: Routes = [
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },

  // Auth routes — redirect away if already logged in
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./auth/auth.routes').then(m => m.AUTH_ROUTES)
  },

  // Customer routes — requires login
  {
    path: 'customer',
    canActivate: [authGuard],
    loadChildren: () => import('./customer/customer.routes').then(m => m.CUSTOMER_ROUTES)
  },

  // Admin + Delivery routes — requires login + role check
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_DELIVERY'] },
    loadChildren: () => import('./admin/admin.routes').then(m => m.ADMIN_ROUTES)
  },

  // Fallback — unknown routes go to login (not a redirect loop since /auth/login now resolves)
  { path: '**', redirectTo: '/auth/login', pathMatch: 'full' }
];
