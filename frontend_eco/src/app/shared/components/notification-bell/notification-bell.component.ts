import { Component, HostListener, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule, ArrowRight, Bell, Inbox } from 'lucide-angular';

import { AdminNotificationService } from '../../../core/services/admin-notification.service';
import { OrderNotification } from '../../../core/models';

/**
 * Chuông báo đơn cần duyệt trên thanh header của khu vực Admin.
 * Toàn bộ dữ liệu lấy từ AdminNotificationService, component chỉ lo phần hiển thị.
 */
@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [DecimalPipe, LucideAngularModule],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.scss',
})
export class NotificationBellComponent {
  readonly BellIcon = Bell;
  readonly InboxIcon = Inbox;
  readonly ArrowRightIcon = ArrowRight;

  private readonly router = inject(Router);
  readonly notifications = inject(AdminNotificationService);

  readonly isOpen = signal(false);

  toggle(event: MouseEvent): void {
    event.stopPropagation();
    const opening = !this.isOpen();
    this.isOpen.set(opening);
    if (opening) this.notifications.markAllSeen();
  }

  /** Bấm ra ngoài thì đóng bảng thông báo. */
  @HostListener('document:click')
  close(): void {
    this.isOpen.set(false);
  }

  openOrder(order: OrderNotification): void {
    this.isOpen.set(false);
    this.router.navigate(['/admin/orders'], { queryParams: { order: order.orderId } });
  }

  openOrderList(): void {
    this.isOpen.set(false);
    this.router.navigate(['/admin/orders'], { queryParams: { status: 'PENDING' } });
  }

  shortId(orderId: string): string {
    return orderId.slice(0, 8).toUpperCase();
  }

  timeAgo(createdAt: string): string {
    const minutes = Math.floor((Date.now() - new Date(createdAt).getTime()) / 60000);
    if (minutes < 1) return 'vừa xong';
    if (minutes < 60) return `${minutes} phút trước`;
    if (minutes < 1440) return `${Math.floor(minutes / 60)} giờ trước`;
    return `${Math.floor(minutes / 1440)} ngày trước`;
  }

  /** Đơn thanh toán online chưa nhận được tiền thì Admin chưa duyệt được. */
  isAwaitingPayment(order: OrderNotification): boolean {
    return order.paymentMethod !== 'COD' && order.paymentStatus !== 'PAID';
  }
}
