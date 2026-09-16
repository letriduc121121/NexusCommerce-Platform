import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { LucideAngularModule, AlertCircle, Calendar, CheckCircle, ChevronRight, Clock, CreditCard, Package, Truck, X } from 'lucide-angular';

import { OrderService } from '../../../core/services/order.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order } from '../../../core/models';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

/** Đơn còn ở 2 trạng thái này thì khách được tự huỷ - khớp với quy tắc ở order-service. */
const CUSTOMER_CANCELLABLE_STATUSES = new Set(['PENDING', 'CONFIRMED']);

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, LucideAngularModule, ModalComponent],
  templateUrl: './order-history.component.html',
  styleUrl: './order-history.component.scss',
})
export class OrderHistoryComponent implements OnInit {
  readonly PackageIcon = Package;
  readonly CalendarIcon = Calendar;
  readonly CreditCardIcon = CreditCard;
  readonly ChevronRightIcon = ChevronRight;
  readonly CheckCircleIcon = CheckCircle;
  readonly TruckIcon = Truck;
  readonly ClockIcon = Clock;
  readonly AlertCircleIcon = AlertCircle;
  readonly XIcon = X;

  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);

  readonly orders = signal<Order[]>([]);
  readonly cancelTarget = signal<Order | null>(null);
  readonly isCancelling = signal(false);

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: data => this.orders.set(data ?? []),
      error: () => this.orders.set([]),
    });
  }

  /** Khách chỉ tự huỷ được khi đơn chưa được giao cho đơn vị vận chuyển. */
  canCancel(order: Order): boolean {
    return CUSTOMER_CANCELLABLE_STATUSES.has(order.status);
  }

  requestCancel(order: Order): void {
    this.cancelTarget.set(order);
  }

  confirmCancel(): void {
    const order = this.cancelTarget();
    if (!order) return;

    this.isCancelling.set(true);
    this.orderService.cancelOrder(order.id).subscribe({
      next: () => {
        this.isCancelling.set(false);
        this.cancelTarget.set(null);
        this.toastService.showSuccess('Đã huỷ đơn hàng', `Đơn #${order.id.slice(0, 8).toUpperCase()} đã được huỷ`);
        this.loadOrders();
      },
      error: err => {
        this.isCancelling.set(false);
        this.cancelTarget.set(null);
        this.toastService.showError('Không thể huỷ đơn', err?.error?.message || 'Vui lòng thử lại sau');
      },
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'DELIVERED':
      case 'CONFIRMED':
        return 'status-success';
      case 'SHIPPING':
        return 'status-shipping';
      case 'PENDING':
        return 'status-pending';
      default:
        return 'status-canceled';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'DELIVERED':
        return 'Đã giao hàng';
      case 'CONFIRMED':
        return 'Đã xác nhận';
      case 'SHIPPING':
        return 'Đang vận chuyển';
      case 'PENDING':
        return 'Đang xử lý';
      case 'CANCELLED':
        return 'Đã hủy';
      default:
        return status;
    }
  }
}
