import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';

import { Order, OrderNotification } from '../models';
import { OrderService } from './order.service';
import { ToastService } from './toast.service';
import { WebsocketService } from './websocket.service';

/**
 * Nguồn dữ liệu duy nhất cho chuông thông báo của Admin.
 * <p>
 * Danh sách chuông = các đơn đang ở trạng thái PENDING (đơn cần duyệt). Nạp lần đầu bằng REST
 * nên F5 vẫn còn, sau đó cập nhật bằng WebSocket nên không cần gọi lại server mỗi lần có biến động.
 */
@Injectable({ providedIn: 'root' })
export class AdminNotificationService {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly websocketService = inject(WebsocketService);

  private subscription?: Subscription;

  /** Đơn chờ duyệt, mới nhất lên đầu. */
  readonly pendingOrders = signal<OrderNotification[]>([]);

  /** Đơn vừa về qua WebSocket mà Admin chưa mở chuông xem -> dùng để đánh dấu "MỚI". */
  readonly unseenIds = signal<ReadonlySet<string>>(new Set());

  readonly connected = this.websocketService.connected;
  readonly pendingCount = computed(() => this.pendingOrders().length);
  readonly unseenCount = computed(() => this.unseenIds().size);

  /** Màn hình khác (bảng quản lý đơn) nghe kênh này để tự làm mới theo thời gian thực. */
  get changes$(): Observable<OrderNotification> {
    return this.websocketService.notifications$;
  }

  // ==================== VÒNG ĐỜI ====================

  start(): void {
    if (this.subscription) return;

    this.reload();
    this.websocketService.connect();
    this.subscription = this.websocketService.notifications$.subscribe(n => this.apply(n));
    this.askBrowserPermission();
  }

  stop(): void {
    this.subscription?.unsubscribe();
    this.subscription = undefined;
    this.websocketService.disconnect();
  }

  /** Nạp lại danh sách chờ duyệt từ server (lúc khởi động, hoặc sau khi Admin duyệt xong một đơn). */
  reload(): void {
    this.orderService.getAllOrders('PENDING').subscribe({
      next: orders => this.pendingOrders.set((orders ?? []).map(toNotification)),
      error: () => this.pendingOrders.set([]),
    });
  }

  markAllSeen(): void {
    this.unseenIds.set(new Set());
  }

  isUnseen(orderId: string): boolean {
    return this.unseenIds().has(orderId);
  }

  // ==================== XỬ LÝ SỰ KIỆN ====================

  private apply(notification: OrderNotification): void {
    // Đơn đã rời khỏi PENDING (được duyệt / bị hủy) thì bỏ khỏi chuông.
    const remaining = this.pendingOrders().filter(o => o.orderId !== notification.orderId);
    this.pendingOrders.set(
      notification.status === 'PENDING' ? [notification, ...remaining] : remaining
    );

    if (notification.type !== 'NEW_ORDER') return;

    this.unseenIds.update(ids => new Set(ids).add(notification.orderId));
    this.alert(notification);
  }

  private alert(notification: OrderNotification): void {
    const amount = notification.totalAmount.toLocaleString('vi-VN');
    this.toastService.showInfo(
      '🔔 Đơn hàng mới',
      `${notification.customerName} vừa đặt đơn ${amount} ₫ - đang chờ duyệt`
    );
    this.playSound();
    this.showBrowserNotification(notification.customerName, amount);
  }

  /** Tạo tiếng "ting" bằng Web Audio, khỏi phải kèm file âm thanh vào bundle. */
  private playSound(): void {
    try {
      const context = new AudioContext();
      const oscillator = context.createOscillator();
      const gain = context.createGain();

      oscillator.connect(gain);
      gain.connect(context.destination);
      oscillator.frequency.value = 880;
      gain.gain.setValueAtTime(0.12, context.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, context.currentTime + 0.4);

      oscillator.start();
      oscillator.stop(context.currentTime + 0.4);
      oscillator.onended = () => context.close();
    } catch {
      // Trình duyệt chặn phát tiếng khi chưa có thao tác người dùng - không sao, vẫn còn toast.
    }
  }

  private askBrowserPermission(): void {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }

  private showBrowserNotification(customerName: string, amount: string): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('Đơn hàng mới cần duyệt', {
        body: `${customerName} - ${amount} ₫`,
        icon: '/favicon.ico',
        tag: 'aula-new-order',
      });
    }
  }
}

/** Đơn lấy từ REST được quy về cùng một hình dạng với gói tin WebSocket. */
function toNotification(order: Order): OrderNotification {
  return {
    type: 'NEW_ORDER',
    orderId: order.id,
    customerName: order.customerName,
    phone: order.phone,
    totalAmount: order.totalAmount,
    status: order.status,
    paymentStatus: order.paymentStatus,
    paymentMethod: order.paymentMethod,
    itemCount: order.orderItems?.length ?? 0,
    createdAt: order.createdAt,
  };
}
