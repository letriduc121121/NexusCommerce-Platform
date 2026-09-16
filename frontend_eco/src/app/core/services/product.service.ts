import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Category, Product, ProductRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  // --- Công khai ---

  /** Backend trả thẳng List<Product>, không phân trang. */
  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(API_ENDPOINTS.products.root);
  }

  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(API_ENDPOINTS.products.byId(id));
  }

  getProductsByCategory(categoryId: number): Observable<Product[]> {
    return this.http.get<Product[]>(API_ENDPOINTS.products.byCategory(categoryId));
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(API_ENDPOINTS.categories.root);
  }

  // --- Dành cho ADMIN ---

  createProduct(payload: ProductRequest): Observable<Product> {
    return this.http.post<Product>(API_ENDPOINTS.products.root, payload);
  }

  updateProduct(id: string, payload: ProductRequest): Observable<Product> {
    return this.http.put<Product>(API_ENDPOINTS.products.byId(id), payload);
  }

  /** Backend trả plain text nên phải khai báo responseType 'text'. */
  deleteProduct(id: string): Observable<string> {
    return this.http.delete(API_ENDPOINTS.products.byId(id), { responseType: 'text' });
  }
}
