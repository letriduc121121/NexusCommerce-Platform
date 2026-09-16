import { Injectable } from '@angular/core';

import { CartItem } from '../models';

const KEY = 'guest_cart';

/** Giỏ hàng khách vãng lai (chưa đăng nhập) - lưu trong localStorage trình duyệt. */
@Injectable({ providedIn: 'root' })
export class GuestCartStore {

  read(): CartItem[] {
    try {
      return JSON.parse(localStorage.getItem(KEY) ?? '[]');
    } catch {
      return [];
    }
  }

  private write(items: CartItem[]): void {
    localStorage.setItem(KEY, JSON.stringify(items));
  }

  /** Thêm mới hoặc cộng dồn số lượng. */
  add(item: CartItem): CartItem[] {
    const items = this.read();
    const found = items.find(i => i.productId === item.productId);
    if (found) {
      found.quantity += item.quantity;
    } else {
      items.push({ ...item });
    }
    this.write(items);
    return items;
  }

  update(productId: string, quantity: number): CartItem[] {
    let items = this.read();
    if (quantity <= 0) {
      items = items.filter(i => i.productId !== productId);
    } else {
      const found = items.find(i => i.productId === productId);
      if (found) found.quantity = quantity;
    }
    this.write(items);
    return items;
  }

  remove(productId: string): CartItem[] {
    const items = this.read().filter(i => i.productId !== productId);
    this.write(items);
    return items;
  }

  clear(): void {
    localStorage.removeItem(KEY);
  }
}
