import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

// Protects routes that require login — redirects to /auth/login if not authenticated
export const authGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);

  if (token.isLoggedIn()) return true;
  return router.createUrlTree(['/auth/login']);
};

// Protects routes that require specific roles
// Usage: canActivate: [roleGuard], data: { roles: ['ROLE_ADMIN'] }
export const roleGuard: CanActivateFn = (route) => {
  const token = inject(TokenService);
  const router = inject(Router);

  const required: string[] = route.data['roles'] ?? [];
  const userRoles = token.getRoles();
  const hasAccess = required.some(r => userRoles.includes(r));

  if (hasAccess) return true;

  // Redirect to appropriate home based on actual role
  if (token.isAdmin()) return router.createUrlTree(['/admin/dashboard']);
  if (token.isCustomer()) return router.createUrlTree(['/customer/home']);
  return router.createUrlTree(['/auth/login']);
};

// Prevents logged-in users from accessing /auth pages
export const guestGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);

  if (!token.isLoggedIn()) return true;

  // Already logged in — redirect to appropriate home
  if (token.isAdmin()) return router.createUrlTree(['/admin/dashboard']);
  if (token.isDelivery()) return router.createUrlTree(['/admin/batches']);
  return router.createUrlTree(['/customer/home']);
};
