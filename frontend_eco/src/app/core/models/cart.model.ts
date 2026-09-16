export interface CartItem {
  id?: number | string;
  productId: string;
  productName: string;
  imageUrl?: string;
  price: number;
  quantity: number;
  subtotal?: number;
  available?: number;
  enough?: boolean;
}

export interface Cart {
  id?: number | string;
  userId?: string | number;
  items: CartItem[];
  totalPrice?: number;
  totalAmount?: number;
}

export interface AddToCartRequest {
  /** Backend nhận UUID nên phải là string. */
  productId: string;
  quantity: number;
  /** Chỉ dùng cho giỏ khách vãng lai (localStorage) để hiển thị - backend bỏ qua. */
  productName?: string;
  price?: number;
  imageUrl?: string;
}

