import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { LucideAngularModule, Ban, Check, CheckCheck, Eye, RefreshCw, Search, Truck, X } from 'lucide-angular';

import { AdminNotificationService } from '../../../core/services/admin-notification.service';
import { OrderService } from '../../../core/services/order.service';
import { ToastService } from '../../../core/services/toast.service';
import { ORDER_STATUSES, Order, OrderStatus } from '../../../core/models';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

/** Nhãn tiếng Việt cho từng trạng thái, dùng chung cho bộ lọc và bảng. */
const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Chờ duyệt',
  CONFIRMED: 'Đã duyệt',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

const STATUS_BADGES: Record<OrderStatus, string> = {
  PENDING: 'badge-warning',
  CONFIRMED: 'badge-info',
  SHIPPING: 'badge-info',
  DELIVERED: 'badge-success',
  CANCELLED: 'badge-danger',
};

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule, LucideAngularModule, ModalComponent, PaginationComponent],
  templateUrl: './order-management.component.html',
  styleUrl: './order-management.component.scss',
})
export class OrderManagementComponent implements OnInit, OnDestroy {
  readonly SearchIcon = Search;
  readonly RefreshIcon = RefreshCw;
  readonly EyeIcon = Eye;
  readonly BanIcon = Ban;
  readonly CheckIcon = Check;
  readonly CheckCheckIcon = CheckCheck;
  readonly TruckIcon = Truck;
  readonly XIcon = X;
  readonly statuses = ORDER_STATUSES;

  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy$ = new Subject<void>();

  readonly notifications = inject(AdminNotificationService);

  readonly orders = signal<Order[]>([]);
  readonly keyword = signal('');
  readonly statusFilter = signal<'ALL' | OrderStatus>('ALL');
  readonly loading = signal(false);
  readonly busyId = signal<string | null>(null);
  readonly page = signal(1);
  readonly detail = signal<Order | null>(null);
  readonly approveAllOpen = signal(false);
  readonly approvingAll = signal(false);
  readonly pageSize = 10;

  /** Đơn cần mở chi tiết ngay khi tải xong (đi từ chuông thông báo sang). */
  private focusOrderId: string | null = null;

  readonly filtered = computed(() => {
    const keyword = this.keyword().toLowerCase().trim();
    const status = this.statusFilter();

    return this.orders().filter(order => {
      if (status !== 'ALL' && order.status !== status) return false;
      if (!keyword) return true;
      return [order.id, order.customerName, order.phone, order.shippingAddress]
        .some(field => field?.toLowerCase().includes(keyword));
    });
  });

  readonly paged = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.loadOrders();

    // Chuông điều hướng sang đây kèm ?order= / ?status= -> nhảy thẳng tới đơn cần xử lý.
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const status = params.get('status') as OrderStatus | null;
      if (status) this.filter(this.keyword(), status);

      this.focusOrderId = params.get('order');
      this.focusOrder();
    });

    // Có biến động đơn hàng từ server -> bảng tự làm mới, admin không phải bấm "Làm mới".
    this.notifications.changes$.pipe(takeUntil(this.destroy$)).subscribe(() => this.loadOrders());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.orderService.getAllOrders().subscribe({
      next: orders => {
        // Mới nhất trên đầu -> vị trí không đổi sau khi đổi trạng thái.
        this.orders.set([...(orders ?? [])].sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '')));
        this.loading.set(false);
        this.focusOrder();
      },
      error: () => {
        this.loading.set(false);
        this.toastService.showError('Lỗi tải dữ liệu', 'Không lấy được danh sách đơn hàng');
      },
    });
  }

  filter(keyword: string, status: 'ALL' | OrderStatus): void {
    this.keyword.set(keyword);
    this.statusFilter.set(status);
    this.page.set(1);
  }

  /** Duyệt / chuyển bước giao hàng / hủy - dùng chung một đường đi để xử lý lỗi một chỗ. */
  changeStatus(order: Order, status: OrderStatus): void {
    this.busyId.set(order.id);

    this.orderService.updateStatus(order.id, status).subscribe({
      next: () => {
        this.busyId.set(null);
        this.toastService.showSuccess(
          'Cập nhật thành công',
          `Đơn #${this.shortId(order.id)} → ${STATUS_LABELS[status]}`
        );
        this.loadOrders();
        this.notifications.reload();
      },
      error: err => {
        this.busyId.set(null);
        this.toastService.showError('Không thể cập nhật', err?.error?.message || 'Vui lòng thử lại sau');
      },
    });
  }

  /** Duyệt một lượt mọi đơn PENDING đủ điều kiện - thao tác nhanh cho Admin khỏi bấm từng đơn. */
  approveAll(): void {
    this.approvingAll.set(true);

    this.orderService.approveAllPending().subscribe({
      next: result => {
        this.approvingAll.set(false);
        this.approveAllOpen.set(false);
        this.toastService.showSuccess(
          'Đã duyệt hàng loạt',
          result.skippedCount > 0
            ? `Duyệt ${result.approvedCount} đơn, bỏ qua ${result.skippedCount} đơn chờ thanh toán online`
            : `Đã duyệt ${result.approvedCount} đơn hàng`
        );
        this.loadOrders();
        this.notifications.reload();
      },
      error: err => {
        this.approvingAll.set(false);
        this.approveAllOpen.set(false);
        this.toastService.showError('Không thể duyệt hàng loạt', err?.error?.message || 'Vui lòng thử lại sau');
      },
    });
  }

  // ==================== QUY TẮC NGHIỆP VỤ (khớp với máy trạng thái ở order-service) ====================

  canConfirm(order: Order): boolean {
    return order.status === 'PENDING' && !this.isAwaitingPayment(order);
  }

  canShip(order: Order): boolean {
    return order.status === 'CONFIRMED';
  }

  canDeliver(order: Order): boolean {
    return order.status === 'SHIPPING';
  }

  canCancel(order: Order): boolean {
    return order.status === 'PENDING' || order.status === 'CONFIRMED';
  }

  /** Đơn thanh toán online chưa nhận được tiền thì backend từ chối duyệt. */
  isAwaitingPayment(order: Order): boolean {
    return order.paymentMethod !== 'COD' && order.paymentStatus !== 'PAID';
  }

  // ==================== HIỂN THỊ ====================

  shortId(orderId: string): string {
    return orderId.slice(0, 8).toUpperCase();
  }

  statusLabel(status: OrderStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  statusClass(status: OrderStatus): string {
    return `badge ${STATUS_BADGES[status] ?? ''}`;
  }

  paymentClass(paymentStatus: string): string {
    if (paymentStatus === 'PAID') return 'badge badge-success';
    if (paymentStatus === 'FAILED') return 'badge badge-danger';
    return 'badge badge-warning';
  }

  private focusOrder(): void {
    if (!this.focusOrderId) return;

    const order = this.orders().find(o => o.id === this.focusOrderId);
    if (order) {
      this.detail.set(order);
      this.focusOrderId = null;
    }
  }
}
