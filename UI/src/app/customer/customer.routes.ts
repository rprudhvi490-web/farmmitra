import { Routes } from '@angular/router';

export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/customer-layout.component').then(m => m.CustomerLayoutComponent),
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      {
        path: 'home',
        loadComponent: () => import('./home/home.component').then(m => m.HomeComponent)
      },
      {
        path: 'products',
        loadComponent: () => import('./products/products.component').then(m => m.ProductsComponent)
      },
      {
        path: 'cart',
        loadComponent: () => import('./cart/cart.component').then(m => m.CartComponent)
      },
      {
        path: 'checkout',
        loadComponent: () => import('./checkout/checkout.component').then(m => m.CheckoutComponent)
      },
      {
        path: 'orders',
        loadComponent: () => import('./orders/orders.component').then(m => m.OrdersComponent)
      },
      {
        path: 'orders/:id',
        loadComponent: () => import('./orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'notifications',
        loadComponent: () => import('./notifications/notifications.component').then(m => m.NotificationsComponent)
      },
      {
        path: 'referral',
        loadComponent: () => import('./referral/referral.component').then(m => m.ReferralComponent)
      },
      { path: '**', redirectTo: 'home' }
    ]
  }
];
