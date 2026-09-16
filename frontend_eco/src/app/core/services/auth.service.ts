import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of, map } from 'rxjs';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { CartService } from './cart.service';
import { TokenService } from './token.service';
import {
  UserProfileResponse,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UpdateProfileRequest,
  ChangePasswordRequest
} from '../models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private cartService = inject(CartService);
  private router = inject(Router);

  isLoggedIn = signal<boolean>(!!this.tokenService.getAccessToken());
  currentUser = signal<UserProfileResponse | null>(null);

  constructor() {
    if (this.isLoggedIn()) {
      this.fetchProfile().subscribe();
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(API_ENDPOINTS.auth.login, credentials).pipe(
      tap(res => {
        if (res && res.token) {
          this.onAuthenticated(res.token, res.refreshToken);
        }
      })
    );
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(API_ENDPOINTS.auth.register, payload).pipe(
      tap(res => {
        if (res && res.token) {
          this.onAuthenticated(res.token, res.refreshToken);
        }
      })
    );
  }

  /** Sau khi đăng nhập / đăng ký thành công: lưu token, nạp hồ sơ, gộp giỏ khách vãng lai. */
  private onAuthenticated(token: string, refreshToken?: string): void {
    this.tokenService.setTokens(token, refreshToken);
    this.isLoggedIn.set(true);
    this.fetchProfile().subscribe();
    this.cartService.mergeGuestCart().subscribe({ error: () => {} });
  }

  fetchProfile(): Observable<UserProfileResponse | null> {
    return this.http.get<UserProfileResponse>(API_ENDPOINTS.users.profile).pipe(
      tap(user => this.currentUser.set(user)),
      catchError(() => {
        this.currentUser.set(null);
        return of(null);
      })
    );
  }

  updateProfile(payload: UpdateProfileRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(API_ENDPOINTS.users.profile, payload).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  /** Backend trả về plain text nên phải dùng responseType 'text'. */
  changePassword(payload: ChangePasswordRequest): Observable<string> {
    return this.http.put(API_ENDPOINTS.users.changePassword, payload, { responseType: 'text' });
  }

  logout(): void {
    const refreshToken = this.tokenService.getRefreshToken() || '';
    this.http.post(API_ENDPOINTS.auth.logout, { refreshToken }).subscribe({
      next: () => this.clearSession(),
      error: () => this.clearSession()
    });
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  private clearSession(): void {
    this.tokenService.clear();
    this.isLoggedIn.set(false);
    this.currentUser.set(null);
    this.cartService.clearLocal();
    this.router.navigate(['/login']);
  }
}

