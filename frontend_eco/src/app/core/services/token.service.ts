import { Injectable } from '@angular/core';
import { STORAGE_KEYS } from '../constants/storage-keys';

/** Bọc localStorage để phần còn lại của app không đụng trực tiếp vào key. */
@Injectable({ providedIn: 'root' })
export class TokenService {
  getAccessToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.accessToken);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.refreshToken);
  }

  setTokens(accessToken: string, refreshToken?: string): void {
    localStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
    if (refreshToken) {
      localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
    }
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEYS.accessToken);
    localStorage.removeItem(STORAGE_KEYS.refreshToken);
    localStorage.removeItem(STORAGE_KEYS.user);
  }
}
