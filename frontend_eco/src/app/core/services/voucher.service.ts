import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { HttpParams } from '@angular/common/http';

import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Voucher, VoucherPreview, VoucherRequest, VoucherStatus } from '../models';

@Injectable({ providedIn: 'root' })
export class VoucherService {
  private readonly http = inject(HttpClient);

  // ===== Admin =====
  getAll(): Observable<Voucher[]> {
    return this.http.get<Voucher[]>(API_ENDPOINTS.vouchers.root);
  }

  create(body: VoucherRequest): Observable<Voucher> {
    return this.http.post<Voucher>(API_ENDPOINTS.vouchers.root, body);
  }

  update(id: number, body: VoucherRequest): Observable<Voucher> {
    return this.http.put<Voucher>(API_ENDPOINTS.vouchers.byId(id), body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(API_ENDPOINTS.vouchers.byId(id));
  }

  /** Tạm dừng / kích hoạt voucher. */
  setStatus(id: number, status: VoucherStatus): Observable<Voucher> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<Voucher>(API_ENDPOINTS.vouchers.status(id), {}, { params });
  }

  // ===== Khách hàng =====

  /** Danh sách mã đang phát (trang chủ). */
  available(): Observable<Voucher[]> {
    return this.http.get<Voucher[]>(API_ENDPOINTS.vouchers.available);
  }

  preview(code: string, userId: string, subtotal: number): Observable<VoucherPreview> {
    return this.http.post<VoucherPreview>(API_ENDPOINTS.vouchers.preview, { code, userId, subtotal });
  }
}
