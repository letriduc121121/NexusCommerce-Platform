import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  ShoppingBag,
  Truck,
  CreditCard,
  User,
  Phone,
  Mail,
  MapPin,
  FileText,
  CheckCircle,
  ArrowLeft,
  Loader2,
  Package,
  Coins,
} from 'lucide-angular';

import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { ToastService } from '../../core/services/toast.service';
import { VoucherService } from '../../core/services/voucher.service';
import { CreateOrderRequest, UserProfileResponse } from '../../core/models';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, LucideAngularModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements OnInit {
  readonly ShoppingBagIcon = ShoppingBag;
  readonly TruckIcon = Truck;
  readonly CreditCardIcon = CreditCard;
  readonly UserIcon = User;
  readonly PhoneIcon = Phone;
  readonly MailIcon = Mail;
  readonly MapPinIcon = MapPin;
  readonly FileTextIcon = FileText;
  readonly CheckCircleIcon = CheckCircle;
  readonly ArrowLeftIcon = ArrowLeft;
  readonly Loader2Icon = Loader2;
  readonly PackageIcon = Package;
  readonly CoinsIcon = Coins;

  authService = inject(AuthService);
  cartService = inject(CartService);
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);
  private toastService = inject(ToastService);
  private voucherService = inject(VoucherService);
  private router = inject(Router);

  // Form Fields
  customerName = signal('');
  phone = signal('');
  customerEmail = signal('');
  shippingAddress = signal('');
  note = signal('');
  paymentMethod = signal<'COD' | 'VNPAY'>('COD');

  isSubmitting = signal(false);

  // ===== Voucher =====
  voucherInput = signal('');
  appliedCode = signal('');
  discountAmount = signal(0);
  voucherMessage = signal('');
  isCheckingVoucher = signal(false);

  // ===== Xu Tích Lũy =====
  useCoins = signal(false);
  userCoins = signal(0);

  /** Số tiền giảm giá từ Xu: 1 Xu = 1 VNĐ, tối đa không quá tổng tiền sau voucher. */
  coinsDiscount = computed(() => {
    if (!this.useCoins()) return 0;
    const currentSub = Math.max(0, this.cartService.totalAmount() - this.discountAmount());
    return Math.min(currentSub, this.userCoins());
  });

  /** Tổng phải trả sau khi trừ Voucher và Xu thưởng. */
  finalTotal = computed(() =>
    Math.max(0, this.cartService.totalAmount() - this.discountAmount() - this.coinsDiscount())
  );

  applyVoucher(): void {
    const code = this.voucherInput().trim();
    const userId = this.authService.currentUser()?.id;
    if (!code || !userId) return;

    this.isCheckingVoucher.set(true);
    this.voucherMessage.set('');

    this.voucherService.preview(code, userId, this.cartService.totalAmount()).subscribe({
      next: (res) => {
        this.isCheckingVoucher.set(false);
        this.appliedCode.set(res.voucherCode);
        this.discountAmount.set(res.discountAmount);
        this.voucherMessage.set(res.message);
      },
      error: (err) => {
        this.isCheckingVoucher.set(false);
        this.clearVoucher();
        this.voucherMessage.set(err?.error?.message || 'Mã không hợp lệ.');
      },
    });
  }

  clearVoucher(): void {
    this.appliedCode.set('');
    this.discountAmount.set(0);
    this.voucherInput.set('');
    this.voucherMessage.set('');
  }

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.toastService.showWarning(
        'Yêu cầu đăng nhập',
        'Vui lòng đăng nhập để tiến hành đặt hàng'
      );
      this.router.navigate(['/login']);
      return;
    }

    const user = this.authService.currentUser();
    if (user) {
      this.customerName.set(user.fullName || '');
      this.phone.set(user.phone || '');
      this.customerEmail.set(user.email || '');
      this.shippingAddress.set(user.address || '');
    }

    // Load profile mới nhất để lấy số Xu tích lũy
    this.authService.fetchProfile().subscribe({
      next: (p: UserProfileResponse | null) => {
        if (p?.rewardCoins) this.userCoins.set(p.rewardCoins);
      },
    });

    this.cartService.loadCart().subscribe({ error: () => {} });
  }

  onSubmit(): void {
    const cart = this.cartService.cart();
    if (!cart || !cart.items || cart.items.length === 0) {
      this.toastService.showWarning(
        'Giỏ hàng trống',
        'Vui lòng thêm sản phẩm vào giỏ hàng trước khi đặt'
      );
      return;
    }

    if (!this.customerName().trim()) {
      this.toastService.showWarning('Thiếu thông tin', 'Vui lòng nhập họ và tên người nhận');
      return;
    }

    if (!this.phone().trim()) {
      this.toastService.showWarning('Thiếu thông tin', 'Vui lòng nhập số điện thoại nhận hàng');
      return;
    }

    if (!this.shippingAddress().trim()) {
      this.toastService.showWarning('Thiếu thông tin', 'Vui lòng nhập địa chỉ giao hàng');
      return;
    }

    const payload: CreateOrderRequest = {
      customerName: this.customerName().trim(),
      phone: this.phone().trim(),
      customerEmail: this.customerEmail().trim() || undefined,
      shippingAddress: this.shippingAddress().trim(),
      note: this.note().trim() || undefined,
      paymentMethod: this.paymentMethod(),
      voucherCode: this.appliedCode() || undefined,
      useCoins: this.useCoins(),
      coinsToUse: this.coinsDiscount(),
      items: cart.items.map((item) => ({
        productId: item.productId || (item.id as string),
        productName: item.productName,
        imageUrl: item.imageUrl,
        price: item.price,
        quantity: item.quantity,
      })),
    };

    this.isSubmitting.set(true);

    this.orderService.createOrder(payload).subscribe({
      next: (order) => {
        if (this.paymentMethod() === 'VNPAY') {
          this.toastService.showInfo('Chuyển hướng', 'Đang chuyển đến cổng thanh toán VNPAY...');
          this.paymentService.getVNPayPaymentUrl(order.id, order.totalAmount).subscribe({
            next: (res) => {
              if (res && res.paymentUrl) {
                window.location.href = res.paymentUrl;
              } else {
                this.isSubmitting.set(false);
                this.toastService.showError(
                  'Lỗi thanh toán',
                  'Không lấy được liên kết VNPAY. Vui lòng thử lại.'
                );
              }
            },
            error: (err) => {
              this.isSubmitting.set(false);
              this.toastService.showError(
                'Lỗi thanh toán',
                err?.error?.message || 'Không thể khởi tạo thanh toán VNPAY'
              );
            },
          });
        } else {
          this.isSubmitting.set(false);
          this.toastService.showSuccess(
            'Đặt hàng thành công',
            `Đơn hàng #${order.id.substring(0, 8)} đã được khởi tạo`
          );
          this.cartService.loadCart().subscribe();
          this.router.navigate(['/checkout/success'], { state: { order } });
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const errorMsg =
          err?.error?.message || 'Không thể khởi tạo đơn hàng. Vui lòng kiểm tra lại.';
        this.toastService.showError('Lỗi đặt hàng', errorMsg);
      },
    });
  }
}
