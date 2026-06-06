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

  // Redirect to their own home
  if (token.isAdmin()) return router.createUrlTree(['/admin/dashboard']);
  if (token.isProcurement()) return router.createUrlTree(['/admin/procurement']);
  if (token.isDelivery()) return router.createUrlTree(['/admin/batches']);
  if (token.isCustomer()) return router.createUrlTree(['/customer/home']);
  return router.createUrlTree(['/auth/login']);
};

// Prevents logged-in users from accessing /auth pages
export const guestGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);

  if (!token.isLoggedIn()) return true;

  if (token.isAdmin()) return router.createUrlTree(['/admin/dashboard']);
  if (token.isDelivery()) return router.createUrlTree(['/admin/batches']);
  if (token.isProcurement()) return router.createUrlTree(['/admin/procurement']);
  return router.createUrlTree(['/customer/home']);
};

// Admin-only route guard
export const adminGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);
  if (token.isAdmin()) return true;
  if (token.isProcurement()) return router.createUrlTree(['/admin/procurement']);
  if (token.isDelivery()) return router.createUrlTree(['/admin/batches']);
  return router.createUrlTree(['/auth/login']);
};

// Procurement-accessible route guard (ADMIN + PROCUREMENT)
export const procurementGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);
  if (token.isAdmin() || token.isProcurement()) return true;
  if (token.isDelivery()) return router.createUrlTree(['/admin/batches']);
  return router.createUrlTree(['/auth/login']);
};

// Delivery-accessible route guard (ADMIN + DELIVERY)
export const deliveryGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);
  if (token.isAdmin() || token.isDelivery()) return true;
  if (token.isProcurement()) return router.createUrlTree(['/admin/procurement']);
  return router.createUrlTree(['/auth/login']);
};
