/** Khớp 1-1 với inventory_service: StockResponse dto. */

export interface StockResponse {
  productId: string;
  productName: string;
  imageUrl?: string;
  quantity: number;
  reserved: number;
  available: number;
  active: boolean;
}
