import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, ArrowRight, CheckCircle, Inbox, Lock, Package, Shield, Users } from 'lucide-angular';

import { AdminNotificationService } from '../../../core/services/admin-notification.service';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { UserProfileResponse } from '../../../core/models';

@Component({
  selector: 'app-dashboard-overview',
  standalone: true,
  imports: [DecimalPipe, RouterLink, LucideAngularModule],
  templateUrl: './dashboard-overview.component.html',
  styleUrl: './dashboard-overview.component.scss',
})
export class DashboardOverviewComponent implements OnInit {
  readonly UsersIcon = Users;
  readonly ShieldIcon = Shield;
  readonly CheckCircleIcon = CheckCircle;
  readonly LockIcon = Lock;
  readonly PackageIcon = Package;
  readonly InboxIcon = Inbox;
  readonly ArrowRightIcon = ArrowRight;

  private readonly adminUserService = inject(AdminUserService);
  private readonly router = inject(Router);

  /** Danh sách đơn chờ duyệt dùng chung với chuông thông báo -> tự cập nhật realtime. */
  readonly notifications = inject(AdminNotificationService);

  readonly users = signal<UserProfileResponse[]>([]);
  readonly isLoading = signal(false);

  readonly totalUsers = computed(() => this.users().length);
  readonly adminCount = computed(() => this.users().filter(u => u.role === 'ADMIN').length);
  readonly activeCount = computed(() => this.users().filter(u => u.status === 'ACTIVE').length);
  readonly lockedCount = computed(() => this.users().filter(u => u.status === 'LOCKED').length);
  readonly recentUsers = computed(() => this.users().slice(0, 5));

  /** Chỉ hiện 5 đơn mới nhất trên dashboard, xem đủ thì sang trang quản lý đơn. */
  readonly recentPendingOrders = computed(() => this.notifications.pendingOrders().slice(0, 5));

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.adminUserService.getAllUsers().subscribe({
      next: data => {
        this.users.set(data ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  openOrder(orderId: string): void {
    this.router.navigate(['/admin/orders'], { queryParams: { order: orderId } });
  }

  shortId(orderId: string): string {
    return orderId.slice(0, 8).toUpperCase();
  }
}
