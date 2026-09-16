import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, of, tap } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api-endpoints';
import { AddToCartRequest, Cart, CartItem } from '../models';
import { GuestCartStore } from './guest-cart.store';
import { TokenService } from './token.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly guestStore = inject(GuestCartStore);
  private readonly tokenService = inject(TokenService);

  private readonly cartState = signal<Cart | null>(null);
  readonly cart = this.cartState.asReadonly();

  readonly itemCount = computed(
    () => this.cartState()?.items?.reduce((sum, item) => sum + item.quantity, 0) ?? 0,
  );

  readonly totalAmount = computed(() => {
    const state = this.cartState();
    if (!state) return 0;
    if (state.totalAmount != null) return state.totalAmount;
    if (state.totalPrice != null) return state.totalPrice;
    return state.items?.reduce((sum, item) => sum + item.price * item.quantity, 0) ?? 0;
  });

  /** Chưa đăng nhập -> thao tác trên giỏ localStorage. */
  private get isGuest(): boolean {
    return !this.tokenService.getAccessToken();
  }

  private buildGuestCart(items: CartItem[]): Cart {
    const enriched = items.map(i => ({ ...i, subtotal: i.price * i.quantity }));
    return {
      items: enriched,
      totalAmount: enriched.reduce((s, i) => s + i.price * i.quantity, 0),
    };
  }

  private setGuest(items: CartItem[]): Cart {
    const cart = this.buildGuestCart(items);
    this.cartState.set(cart);
    return cart;
  }

  loadCart(): Observable<Cart> {
    if (this.isGuest) {
      return of(this.setGuest(this.guestStore.read()));
    }
    return this.http.get<Cart>(API_ENDPOINTS.cart.root).pipe(tap(c => this.cartState.set(c)));
  }

  addItem(payload: AddToCartRequest): Observable<Cart> {
    if (this.isGuest) {
      return of(this.setGuest(this.guestStore.add({
        productId: payload.productId,
        productName: payload.productName ?? '',
        imageUrl: payload.imageUrl,
        price: payload.price ?? 0,
        quantity: payload.quantity,
      })));
    }
    const body = { productId: payload.productId, quantity: payload.quantity };
    return this.http.post<Cart>(API_ENDPOINTS.cart.items, body).pipe(tap(c => this.cartState.set(c)));
  }

  updateItemQuantity(productId: string, quantity: number): Observable<Cart> {
    if (this.isGuest) {
      return of(this.setGuest(this.guestStore.update(productId, quantity)));
    }
    return this.http
      .put<Cart>(`${API_ENDPOINTS.cart.itemById(productId)}?quantity=${quantity}`, {})
      .pipe(tap(c => this.cartState.set(c)));
  }

  removeItem(productId: string): Observable<Cart> {
    if (this.isGuest) {
      return of(this.setGuest(this.guestStore.remove(productId)));
    }
    return this.http
      .delete<Cart>(API_ENDPOINTS.cart.itemById(productId))
      .pipe(tap(c => this.cartState.set(c)));
  }

  clearCart(): Observable<void> {
    if (this.isGuest) {
      this.guestStore.clear();
      this.cartState.set({ items: [], totalAmount: 0 });
      return of(void 0);
    }
    return this.http
      .delete<void>(API_ENDPOINTS.cart.root)
      .pipe(tap(() => this.cartState.set({ items: [], totalAmount: 0 })));
  }

  /** Sau khi đăng nhập: đẩy giỏ khách vãng lai lên server để gộp, rồi cập nhật giỏ đã gộp. */
  mergeGuestCart(): Observable<Cart> {
    const guestItems = this.guestStore.read();
    if (!guestItems.length) {
      return this.loadCart();
    }
    const body = guestItems.map(i => ({ productId: i.productId, quantity: i.quantity }));
    return this.http.post<Cart>(API_ENDPOINTS.cart.merge, body).pipe(
      tap(c => {
        this.guestStore.clear();
        this.cartState.set(c);
      }),
    );
  }

  /** Xoá state trong bộ nhớ (không gọi API) - dùng khi logout. */
  clearLocal(): void {
    this.cartState.set(null);
  }
}
