/** Khớp 1-1 với order_service: entity Order / OrderItem và dto OrderRequest. */

export const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'REFUNDED', 'CANCELLED'] as const;
export type OrderStatus = (typeof ORDER_STATUSES)[number];

export const ORDER_PAYMENT_STATUSES = ['PENDING', 'PAID', 'FAILED'] as const;
export type OrderPaymentStatus = (typeof ORDER_PAYMENT_STATUSES)[number];

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  imageUrl?: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: string;
  userId: string;
  status: OrderStatus;
  subtotal?: number;
  discountAmount?: number;
  coinsUsed?: number;
  coinsDiscountAmount?: number;
  voucherCode?: string;
  totalAmount: number;
  customerName: string;
  phone: string;
  customerEmail?: string;
  shippingAddress: string;
  note?: string;
  shippingFee: number;
  paymentMethod: string;
  paymentStatus: OrderPaymentStatus;
  createdAt: string;
  updatedAt: string;
  orderItems: OrderItem[];
}

export interface CreateOrderItemRequest {
  productId: string;
  productName: string;
  imageUrl?: string;
  price: number;
  quantity: number;
}

export interface CreateOrderRequest {
  customerName: string;
  phone: string;
  customerEmail?: string;
  shippingAddress: string;
  note?: string;
  paymentMethod?: string;
  voucherCode?: string;
  useCoins?: boolean;
  coinsToUse?: number;
  items: CreateOrderItemRequest[];
}

/** Khớp 1-1 với dto BulkApprovalResult của order-service. */
export interface BulkApprovalResult {
  approvedCount: number;
  skippedCount: number;
}
