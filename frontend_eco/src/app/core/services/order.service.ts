import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api-endpoints';
import { BulkApprovalResult, CreateOrderRequest, Order, OrderStatus, OrderPaymentStatus } from '../models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(API_ENDPOINTS.orders.root);
  }

  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(API_ENDPOINTS.orders.byId(id));
  }

  createOrder(payload: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(API_ENDPOINTS.orders.root, payload);
  }

  /** Khách tự huỷ đơn của mình - backend chỉ chấp nhận khi đơn còn PENDING hoặc CONFIRMED. */
  cancelOrder(id: string): Observable<Order> {
    return this.http.put<Order>(API_ENDPOINTS.orders.cancel(id), {});
  }

  // --- Dành cho Admin ---

  /** Bỏ trống status = lấy tất cả đơn; truyền 'PENDING' để lấy riêng các đơn cần duyệt. */
  getAllOrders(status?: OrderStatus): Observable<Order[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Order[]>(API_ENDPOINTS.orders.admin, { params });
  }

  /** Backend nhận status / paymentStatus qua query param, cái nào bỏ trống thì giữ nguyên. */
  updateStatus(id: string, status?: OrderStatus, paymentStatus?: OrderPaymentStatus): Observable<Order> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (paymentStatus) params = params.set('paymentStatus', paymentStatus);
    return this.http.put<Order>(API_ENDPOINTS.orders.status(id), {}, { params });
  }

  /** Duyệt một lượt mọi đơn PENDING đủ điều kiện; đơn thanh toán online chưa có tiền bị bỏ qua. */
  approveAllPending(): Observable<BulkApprovalResult> {
    return this.http.put<BulkApprovalResult>(API_ENDPOINTS.orders.approveAll, {});
  }

  /** Admin xác nhận trả hàng / hoàn tiền (DELIVERED -> REFUNDED). */
  refundOrder(id: string): Observable<Order> {
    return this.http.put<Order>(API_ENDPOINTS.orders.refund(id), {});
  }
}
