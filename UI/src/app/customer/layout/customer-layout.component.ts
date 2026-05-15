import { Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { CartService } from '../services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { NotificationService } from '../services/notification.service';

@Component({
  selector: 'app-customer-layout',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    CommonModule,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatMenuModule
  ],
  templateUrl: './customer-layout.component.html',
  styleUrl: './customer-layout.component.scss'
})
export class CustomerLayoutComponent implements OnInit {
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private tokenService = inject(TokenService);
  private notifService = inject(NotificationService);
  private destroyRef = inject(DestroyRef);

  cartCount = signal(0);
  unreadCount = signal(0);
  phone = this.tokenService.getPhone();
  username = this.tokenService.getUsername();

  ngOnInit(): void {
    this.cartService.items
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(items => this.cartCount.set(items.length));

    this.notifService.getMy()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(n => this.unreadCount.set(n.filter(x => !x.readStatus).length));
  }

  logout(): void {
    this.authService.logout();
  }
}
