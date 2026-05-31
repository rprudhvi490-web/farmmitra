import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard',   loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'cycles',      loadComponent: () => import('./cycles/cycles.component').then(m => m.CyclesComponent) },
      { path: 'cycle-stock',  loadComponent: () => import('./cycle-stock/cycle-stock.component').then(m => m.CycleStockComponent) },
      { path: 'products',    loadComponent: () => import('./products/products.component').then(m => m.AdminProductsComponent) },
      { path: 'categories',  loadComponent: () => import('./categories/categories.component').then(m => m.CategoriesComponent) },
      { path: 'orders',      loadComponent: () => import('./orders/orders.component').then(m => m.AdminOrdersComponent) },
      { path: 'procurement', loadComponent: () => import('./procurement/procurement.component').then(m => m.ProcurementComponent) },
      { path: 'transport',   loadComponent: () => import('./transport/transport.component').then(m => m.TransportComponent) },
      { path: 'batches',     loadComponent: () => import('./batches/batches.component').then(m => m.BatchesComponent) },
      { path: 'users',         loadComponent: () => import('./users/users.component').then(m => m.UsersComponent) },
      { path: 'notifications',  loadComponent: () => import('./notifications/notifications.component').then(m => m.AdminNotificationsComponent) },
      { path: '**', redirectTo: 'dashboard' }
    ]
  }
];
