import { environment } from '../../../environments/environment';

const base = environment.apiUrl;

/** Các endpoint tương ứng với route khai báo trong api-gateway/application.yml. */
export const API_ENDPOINTS = {
  auth: {
    login: `${base}/auth/login`,
    register: `${base}/auth/register`,
    refresh: `${base}/auth/refresh`,
    logout: `${base}/auth/logout`,
  },
  users: {
    root: `${base}/users`,
    byId: (id: string | number) => `${base}/users/${id}`,
    profile: `${base}/users/profile`,
    changePassword: `${base}/users/change-password`,
  },
  products: {
    root: `${base}/products`,
    byId: (id: string | number) => `${base}/products/${id}`,
    search: `${base}/products/search`,
    byCategory: (categoryId: number) => `${base}/products/category/${categoryId}`,
    uploadImage: `${base}/products/upload-image`,
  },
  categories: {
    root: `${base}/categories`,
    byId: (id: string | number) => `${base}/categories/${id}`,
  },
  cart: {
    root: `${base}/cart`,
    items: `${base}/cart/items`,
    itemById: (id: string | number) => `${base}/cart/items/${id}`,
    merge: `${base}/cart/merge`,
  },
  orders: {
    root: `${base}/orders`,
    byId: (id: string | number) => `${base}/orders/${id}`,
    admin: `${base}/orders/admin`,
    approveAll: `${base}/orders/admin/approve-all`,
    status: (id: string | number) => `${base}/orders/${id}/status`,
    cancel: (id: string | number) => `${base}/orders/${id}/cancel`,
    refund: (id: string | number) => `${base}/orders/${id}/refund`,
  },
  payments: {
    root: `${base}/payments`,
    byId: (id: string | number) => `${base}/payments/${id}`,
    vnpayUrl: (orderId: string, amount: number) => `${base}/payments/vnpay-payment-url/${orderId}?amount=${amount}`,
  },
  inventories: {
    root: `${base}/inventories`,
    byProductId: (id: string | number) => `${base}/inventories/${id}`,
  },
  vouchers: {
    root: `${base}/vouchers`,
    byId: (id: number) => `${base}/vouchers/${id}`,
    status: (id: number) => `${base}/vouchers/${id}/status`,
    preview: `${base}/vouchers/preview`,
    available: `${base}/vouchers/available`,
  },
  reviews: {
    root: `${base}/reviews`,
    byId: (id: string) => `${base}/reviews/${id}`,
    byProduct: (productId: string, rating?: number, withImages?: boolean) => {
      let url = `${base}/products/${productId}/reviews`;
      const params: string[] = [];
      if (rating && rating > 0) params.push(`rating=${rating}`);
      if (withImages) params.push(`withImages=true`);
      if (params.length > 0) url += `?${params.join('&')}`;
      return url;
    },
    myReviewedItemIds: `${base}/reviews/my-reviewed-order-item-ids`,
    verifyPurchase: (orderItemId: string, userId: string) =>
      `${base}/orders/verify-purchase?orderItemId=${orderItemId}&userId=${userId}`,
  },
} as const;
