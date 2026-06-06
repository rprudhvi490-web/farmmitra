import { Component, inject, signal, HostListener } from '@angular/core';
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
import { NotificationBannerComponent } from '../../core/components/notification-banner.component';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  procurementVisible?: boolean; // show for PROCUREMENT role
}

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, CommonModule,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule,
    NotificationBannerComponent
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss'
})
export class AdminLayoutComponent {
  private authService = inject(AuthService);
  tokenService = inject(TokenService);

  sidenavOpen = signal(true);
  isMobile = signal(window.innerWidth < 960);

  get sidenavMode(): 'side' | 'over' {
    return this.isMobile() ? 'over' : 'side';
  }

  @HostListener('window:resize')
  onResize(): void {
    const mobile = window.innerWidth < 960;
    this.isMobile.set(mobile);
    if (mobile) this.sidenavOpen.set(false);
    else this.sidenavOpen.set(true);
  }

  private readonly allNavItems: NavItem[] = [
    { label: 'Dashboard',    icon: 'dashboard',        route: '/admin/dashboard' },
    { label: 'Cycles',       icon: 'event_repeat',     route: '/admin/cycles' },
    { label: 'Stock Limits', icon: 'inventory',        route: '/admin/cycle-stock' },
    { label: 'Products',     icon: 'inventory_2',      route: '/admin/products' },
    { label: 'Categories',   icon: 'category',         route: '/admin/categories' },
    { label: 'Orders',       icon: 'receipt_long',     route: '/admin/orders' },
    { label: 'Procurement',  icon: 'agriculture',      route: '/admin/procurement', procurementVisible: true },
    { label: 'Transport',    icon: 'local_shipping',   route: '/admin/transport',   procurementVisible: true },
    { label: 'Delivery',     icon: 'delivery_dining',  route: '/admin/batches' },
    { label: 'Users',        icon: 'people',           route: '/admin/users' },
    { label: 'Notifications', icon: 'campaign',        route: '/admin/notifications' },
  ];

  get navItems(): NavItem[] {
    if (this.tokenService.isProcurement()) {
      return this.allNavItems.filter(i => i.procurementVisible);
    }
    return this.allNavItems;
  }

  get roleLabel(): string {
    if (this.tokenService.hasRole('ROLE_SUPER_ADMIN')) return 'Super Admin';
    if (this.tokenService.isAdmin()) return 'Admin';
    if (this.tokenService.isProcurement()) return 'Procurement';
    if (this.tokenService.isDelivery()) return 'Delivery';
    return 'User';
  }

  toggleSidenav(): void {
    this.sidenavOpen.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }
}
