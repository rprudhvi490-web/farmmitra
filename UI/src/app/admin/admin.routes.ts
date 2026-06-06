import { Routes } from '@angular/router';
import { adminGuard, procurementGuard, deliveryGuard } from '../core/guards/guards';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      // ── ADMIN only ────────────────────────────────────────────────────────
      { path: 'dashboard',     canActivate: [adminGuard], loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'cycles',        canActivate: [adminGuard], loadComponent: () => import('./cycles/cycles.component').then(m => m.CyclesComponent) },
      { path: 'cycle-stock',   canActivate: [adminGuard], loadComponent: () => import('./cycle-stock/cycle-stock.component').then(m => m.CycleStockComponent) },
      { path: 'products',      canActivate: [adminGuard], loadComponent: () => import('./products/products.component').then(m => m.AdminProductsComponent) },
      { path: 'categories',    canActivate: [adminGuard], loadComponent: () => import('./categories/categories.component').then(m => m.CategoriesComponent) },
      { path: 'orders',        canActivate: [adminGuard], loadComponent: () => import('./orders/orders.component').then(m => m.AdminOrdersComponent) },
      { path: 'users',         canActivate: [adminGuard], loadComponent: () => import('./users/users.component').then(m => m.UsersComponent) },
      { path: 'notifications', canActivate: [adminGuard], loadComponent: () => import('./notifications/notifications.component').then(m => m.AdminNotificationsComponent) },

      // ── ADMIN + PROCUREMENT ───────────────────────────────────────────────
      { path: 'procurement',   canActivate: [procurementGuard], loadComponent: () => import('./procurement/procurement.component').then(m => m.ProcurementComponent) },
      { path: 'transport',     canActivate: [procurementGuard], loadComponent: () => import('./transport/transport.component').then(m => m.TransportComponent) },

      // ── ADMIN + DELIVERY ──────────────────────────────────────────────────
      { path: 'batches',       canActivate: [deliveryGuard], loadComponent: () => import('./batches/batches.component').then(m => m.BatchesComponent) },

      { path: '**', redirectTo: 'dashboard' }
    ]
  }
];
