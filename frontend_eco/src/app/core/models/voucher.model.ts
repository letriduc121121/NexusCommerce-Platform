/** Khớp 1-1 với promotion_service. */

export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREE_SHIPPING';
export type VoucherStatus = 'ACTIVE' | 'PAUSED' | 'ENDED';

export interface Voucher {
  id: number;
  code: string;
  name: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount?: number;
  minOrderAmount: number;
  totalQuantity?: number;
  usedCount: number;
  issuedCount: number;
  usableFrom: string;
  usableTo: string;
  status: VoucherStatus;
  createdAt: string;
  updatedAt: string;
}

/** Dữ liệu gửi lên khi tạo / sửa voucher. */
export interface VoucherRequest {
  code: string;
  name: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount?: number | null;
  minOrderAmount: number;
  totalQuantity?: number | null;
  usableFrom: string;
  usableTo: string;
}

/** Kết quả preview mã ở trang thanh toán. */
export interface VoucherPreview {
  valid: boolean;
  voucherCode: string;
  discountType: DiscountType;
  discountAmount: number;
  freeShipping: boolean;
  message: string;
}

export const DISCOUNT_TYPE_LABEL: Record<DiscountType, string> = {
  PERCENTAGE: 'Giảm theo %',
  FIXED_AMOUNT: 'Giảm số tiền',
  FREE_SHIPPING: 'Miễn phí ship',
};

export const VOUCHER_STATUS_LABEL: Record<VoucherStatus, string> = {
  ACTIVE: 'Đang chạy',
  PAUSED: 'Tạm dừng',
  ENDED: 'Đã kết thúc',
};
