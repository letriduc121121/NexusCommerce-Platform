import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../constants/api-endpoints';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);

  getVNPayPaymentUrl(orderId: string, amount: number): Observable<{ paymentUrl: string }> {
    return this.http.get<{ paymentUrl: string }>(API_ENDPOINTS.payments.vnpayUrl(orderId, amount));
  }
}
