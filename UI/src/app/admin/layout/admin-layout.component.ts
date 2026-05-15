import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  superAdminOnly?: boolean;
}

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, CommonModule,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent {
  private authService = inject(AuthService);
  tokenService = inject(TokenService);

  sidenavOpen = signal(true);

  navItems: NavItem[] = [
    { label: 'Dashboard',   icon: 'dashboard',      route: '/admin/dashboard' },
    { label: 'Cycles',      icon: 'event_repeat',   route: '/admin/cycles' },
    { label: 'Products',    icon: 'inventory_2',    route: '/admin/products' },
    { label: 'Categories',  icon: 'category',       route: '/admin/categories' },
    { label: 'Orders',      icon: 'receipt_long',   route: '/admin/orders' },
    { label: 'Procurement', icon: 'agriculture',    route: '/admin/procurement' },
    { label: 'Transport',   icon: 'local_shipping', route: '/admin/transport' },
    { label: 'Delivery',    icon: 'delivery_dining', route: '/admin/batches' },
    { label: 'Users',         icon: 'people',          route: '/admin/users' },
    { label: 'Notifications',  icon: 'campaign',        route: '/admin/notifications' },
  ];

  toggleSidenav(): void {
    this.sidenavOpen.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }
}
