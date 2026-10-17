import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api-endpoints';
import {
  CreateReviewRequest,
  ReviewResponse,
  UpdateReviewRequest,
  VerifyPurchaseResponse,
} from '../models/review.model';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);

  getReviewsByProduct(
    productId: string,
    rating?: number,
    withImages?: boolean
  ): Observable<ReviewResponse[]> {
    return this.http
      .get<any>(API_ENDPOINTS.reviews.byProduct(productId, rating, withImages))
      .pipe(
        map((res) => {
          if (res && Array.isArray(res.content)) {
            return res.content;
          }
          if (Array.isArray(res)) {
            return res;
          }
          return [];
        })
      );
  }

  verifyPurchase(orderItemId: string, userId: string): Observable<VerifyPurchaseResponse> {
    return this.http.get<VerifyPurchaseResponse>(
      API_ENDPOINTS.reviews.verifyPurchase(orderItemId, userId)
    );
  }

  getMyReviewedOrderItemIds(): Observable<string[]> {
    return this.http.get<string[]>(API_ENDPOINTS.reviews.myReviewedItemIds);
  }

  createReview(payload: CreateReviewRequest): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(API_ENDPOINTS.reviews.root, payload);
  }

  uploadImage(file: File): Observable<{ imageUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ imageUrl: string }>(API_ENDPOINTS.products.uploadImage, formData);
  }

  updateReview(reviewId: string, payload: UpdateReviewRequest): Observable<ReviewResponse> {
    return this.http.put<ReviewResponse>(API_ENDPOINTS.reviews.byId(reviewId), payload);
  }
}
