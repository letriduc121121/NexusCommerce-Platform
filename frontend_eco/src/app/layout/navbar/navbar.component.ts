import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { finalize } from 'rxjs';
import {
  LucideAngularModule,
  ShoppingBag,
  User,
  Menu,
  X,
  LogOut,
  Package,
  UserCheck,
  ChevronDown,
  LogIn,
  Shield,
  Trash2,
  Plus,
  Minus,
  ShoppingCart,
  Loader2,
  Coins
} from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { CartItem } from '../../core/models';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLink,
    LucideAngularModule,
    CurrencyPipe
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  authService = inject(AuthService);
  cartService = inject(CartService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  readonly ShoppingBagIcon = ShoppingBag;
  readonly UserIcon = User;
  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly LogOutIcon = LogOut;
  readonly PackageIcon = Package;
  readonly UserCheckIcon = UserCheck;
  readonly ChevronDownIcon = ChevronDown;
  readonly LogInIcon = LogIn;
  readonly ShieldIcon = Shield;
  readonly Trash2Icon = Trash2;
  readonly PlusIcon = Plus;
  readonly MinusIcon = Minus;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly Loader2Icon = Loader2;
  readonly CoinsIcon = Coins;

  isScrolled = signal(false);
  isMobileMenuOpen = signal(false);
  isProfileMenuOpen = signal(false);
  showCartSidebar = signal(false);
  cartItemCount = this.cartService.itemCount;

  loadingProductId = signal<string | null>(null);
  isClearingCart = signal(false);

  ngOnInit(): void {
    // Nạp giỏ cho cả khách vãng lai (localStorage) lẫn user đã đăng nhập.
    this.cartService.loadCart().subscribe({ error: () => {} });
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    this.isScrolled.set(window.scrollY > 10);
  }

  scrollToSection(sectionId: string): void {
    this.closeMobileMenu();
    const currentPath = this.router.url.split('#')[0];
    if (currentPath !== '/') {
      this.router.navigate(['/']).then(() => {
        setTimeout(() => {
          document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' });
        }, 100);
      });
    } else {
      document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' });
    }
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
    this.isProfileMenuOpen.set(false);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  toggleProfileMenu(): void {
    this.isProfileMenuOpen.update(v => !v);
  }

  closeProfileMenu(): void {
    this.isProfileMenuOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.closeProfileMenu();
    this.closeMobileMenu();
  }

  toggleCartSidebar(): void {
    this.showCartSidebar.update(v => !v);
  }

  closeCartSidebar(): void {
    this.showCartSidebar.set(false);
  }

  goToCheckout(): void {
    this.closeCartSidebar();
    this.router.navigate(['/checkout']);
  }

  updateQuantity(item: CartItem, delta: number): void {
    const targetId = item.productId || (item.id as string);
    const newQty = item.quantity + delta;

    if (newQty <= 0) {
      this.removeItem(item);
      return;
    }

    this.loadingProductId.set(targetId);
    this.cartService.updateItemQuantity(targetId, newQty)
      .pipe(finalize(() => this.loadingProductId.set(null)))
      .subscribe({
        error: (err) => this.toastService.showError('Lỗi', err?.error?.message || 'Không thể cập nhật số lượng')
      });
  }

  removeItem(item: CartItem): void {
    const targetId = item.productId || (item.id as string);
    this.loadingProductId.set(targetId);

    this.cartService.removeItem(targetId)
      .pipe(finalize(() => this.loadingProductId.set(null)))
      .subscribe({
        next: () => this.toastService.showSuccess('Đã xóa', `Đã xóa ${item.productName} khỏi giỏ hàng`),
        error: (err) => this.toastService.showError('Lỗi', err?.error?.message || 'Không thể xóa sản phẩm')
      });
  }

  clearCart(): void {
    if (!confirm('Bạn chắc chắn muốn xóa tất cả sản phẩm khỏi giỏ hàng?')) return;

    this.isClearingCart.set(true);
    this.cartService.clearCart()
      .pipe(finalize(() => this.isClearingCart.set(false)))
      .subscribe({
        next: () => this.toastService.showSuccess('Thông báo', 'Đã xóa tất cả sản phẩm khỏi giỏ hàng'),
        error: (err) => this.toastService.showError('Lỗi', err?.error?.message || 'Không thể xóa giỏ hàng')
      });
  }
}


