import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api-endpoints';
import { StockResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);

  getInventories(): Observable<StockResponse[]> {
    return this.http.get<StockResponse[]>(API_ENDPOINTS.inventories.root);
  }

  updateQuantity(productId: string, quantity: number): Observable<StockResponse> {
    const params = new HttpParams().set('quantity', quantity.toString());
    return this.http.put<StockResponse>(API_ENDPOINTS.inventories.byProductId(productId), {}, { params });
  }
}
