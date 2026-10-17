import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CurrencyPipe, DatePipe, CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  AlertCircle,
  Calendar,
  CheckCircle,
  ChevronRight,
  Clock,
  CreditCard,
  Package,
  Truck,
  X,
  Coins,
  Star,
  Camera,
  MessageSquare,
  Sparkles,
} from 'lucide-angular';

import { OrderService } from '../../../core/services/order.service';
import { ReviewService } from '../../../core/services/review.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order, OrderItem } from '../../../core/models';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

const CUSTOMER_CANCELLABLE_STATUSES = new Set(['PENDING', 'CONFIRMED']);

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, FormsModule, LucideAngularModule, ModalComponent],
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
  readonly CoinsIcon = Coins;
  readonly StarIcon = Star;
  readonly CameraIcon = Camera;
  readonly MessageSquareIcon = MessageSquare;
  readonly SparklesIcon = Sparkles;

  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly reviewService = inject(ReviewService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);

  readonly orders = signal<Order[]>([]);
  readonly reviewedOrderItemIds = signal<Set<string>>(new Set());
  readonly cancelTarget = signal<Order | null>(null);
  readonly isCancelling = signal(false);

  ngOnInit(): void {
    this.authService.fetchProfile().subscribe();
    this.loadOrders();
    this.loadReviewedItems();
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: (data) => this.orders.set(data ?? []),
      error: () => this.orders.set([]),
    });
  }

  loadReviewedItems(): void {
    this.reviewService.getMyReviewedOrderItemIds().subscribe({
      next: (ids) => {
        if (Array.isArray(ids)) {
          this.reviewedOrderItemIds.set(new Set(ids.map((id) => String(id).toLowerCase())));
        }
      },
      error: () => {
        this.reviewedOrderItemIds.set(new Set());
      },
    });
  }

  isItemReviewed(orderItemId: string): boolean {
    return this.reviewedOrderItemIds().has(String(orderItemId).toLowerCase());
  }

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
        this.toastService.showSuccess(
          'Đã huỷ đơn hàng',
          `Đơn #${order.id.slice(0, 8).toUpperCase()} đã được huỷ`
        );
        this.loadOrders();
      },
      error: (err) => {
        this.isCancelling.set(false);
        this.cancelTarget.set(null);
        this.toastService.showError(
          'Không thể huỷ đơn',
          err?.error?.message || 'Vui lòng thử lại sau'
        );
      },
    });
  }

  goToReview(item: OrderItem, order: Order): void {
    this.router.navigate(['/review', item.id], {
      state: { item, order },
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
