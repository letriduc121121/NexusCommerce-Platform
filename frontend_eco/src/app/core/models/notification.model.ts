/** Khớp 1-1 với order_service: record OrderNotification đẩy qua WebSocket /topic/admin/orders. */

import { OrderPaymentStatus, OrderStatus } from './order.model';

export type OrderNotificationType = 'NEW_ORDER' | 'ORDER_UPDATED';

export interface OrderNotification {
  type: OrderNotificationType;
  orderId: string;
  customerName: string;
  phone: string;
  totalAmount: number;
  status: OrderStatus;
  paymentStatus: OrderPaymentStatus;
  paymentMethod: string;
  itemCount: number;
  createdAt: string;
}
