import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideAngularModule, ArrowLeft, LayoutDashboard, LogOut, Package, ShoppingBag, Ticket, Users, Warehouse } from 'lucide-angular';

import { AdminNotificationService } from '../../../core/services/admin-notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationBellComponent } from '../../../shared/components/notification-bell/notification-bell.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, LucideAngularModule, NotificationBellComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  readonly LayoutDashboardIcon = LayoutDashboard;
  readonly UsersIcon = Users;
  readonly ShoppingBagIcon = ShoppingBag;
  readonly PackageIcon = Package;
  readonly WarehouseIcon = Warehouse;
  readonly TicketIcon = Ticket;
  readonly LogOutIcon = LogOut;
  readonly ArrowLeftIcon = ArrowLeft;

  readonly authService = inject(AuthService);
  readonly notifications = inject(AdminNotificationService);

  /** Mở kết nối ở tầng layout: chuông sống suốt phiên làm việc, không phụ thuộc trang đang xem. */
  ngOnInit(): void {
    this.notifications.start();
  }

  ngOnDestroy(): void {
    this.notifications.stop();
  }

  logout(): void {
    this.authService.logout();
  }
}
