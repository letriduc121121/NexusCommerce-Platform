export interface Category {
  id: number;
  name: string;
  description?: string;
}

/** Khớp entity Product của product_service: id là UUID, category là object lồng. */
export interface Product {
  id: string;
  category?: Category;
  sku: string;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** Payload admin gửi lên khi tạo / sửa sản phẩm. */
export interface ProductRequest {
  category: { id: number };
  sku: string;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  status?: string;
}
