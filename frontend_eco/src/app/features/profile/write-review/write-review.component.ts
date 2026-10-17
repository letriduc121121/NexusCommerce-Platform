import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  Star,
  Camera,
  Coins,
  Sparkles,
  ArrowLeft,
  CheckCircle,
  AlertTriangle,
  ShoppingBag,
  Info,
  Calendar,
  X,
  Upload,
  Image as ImageIcon,
} from 'lucide-angular';

import { ReviewService } from '../../../core/services/review.service';
import { OrderService } from '../../../core/services/order.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/services/auth.service';
import { Order, OrderItem } from '../../../core/models';

@Component({
  selector: 'app-write-review',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CurrencyPipe, LucideAngularModule],
  templateUrl: './write-review.component.html',
  styleUrl: './write-review.component.scss',
})
export class WriteReviewComponent implements OnInit {
  readonly StarIcon = Star;
  readonly CameraIcon = Camera;
  readonly CoinsIcon = Coins;
  readonly SparklesIcon = Sparkles;
  readonly ArrowLeftIcon = ArrowLeft;
  readonly CheckCircleIcon = CheckCircle;
  readonly AlertTriangleIcon = AlertTriangle;
  readonly ShoppingBagIcon = ShoppingBag;
  readonly InfoIcon = Info;
  readonly CalendarIcon = Calendar;
  readonly XIcon = X;
  readonly UploadIcon = Upload;
  readonly ImageIcon = ImageIcon;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly reviewService = inject(ReviewService);
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);

  readonly orderItemId = signal<string>('');
  readonly orderItem = signal<OrderItem | null>(null);
  readonly order = signal<Order | null>(null);

  readonly isLoading = signal<boolean>(true);
  readonly isSubmitting = signal<boolean>(false);
  readonly isUploadingPhoto = signal<boolean>(false);
  readonly isAlreadyReviewed = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Form State
  readonly rating = signal<number>(5);
  readonly hoverRating = signal<number>(0);
  readonly comment = signal<string>('');
  readonly photoUrl = signal<string>('');

  // Quick review suggestion chips
  readonly quickTags = [
    'Chất vải cao cấp & đẹp',
    'Đúng form dáng mô tả',
    'Giao hàng siêu nhanh 🚀',
    'Đóng gói rất chỉn chu',
    'Shop tư vấn nhiệt tình',
  ];

  readonly currentDisplayRating = computed(() => {
    return this.hoverRating() > 0 ? this.hoverRating() : this.rating();
  });

  readonly predictedCoins = computed(() => {
    const commentLen = this.comment().trim().length;
    const hasPhoto = this.photoUrl().trim().length > 0;
    if (commentLen >= 50 && hasPhoto) return 200;
    if (commentLen >= 50) return 100;
    return 0;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('orderItemId');
    if (!id) {
      this.toastService.showError('Lỗi', 'Không tìm thấy mã sản phẩm cần đánh giá!');
      this.router.navigate(['/profile/orders']);
      return;
    }

    this.orderItemId.set(id);

    // Kiểm tra state từ navigation trước
    const navState = history.state;
    if (navState && navState.item) {
      this.orderItem.set(navState.item);
      if (navState.order) {
        this.order.set(navState.order);
      }
    }

    this.loadAndVerifyData(id);
  }

  loadAndVerifyData(orderItemId: string): void {
    this.isLoading.set(true);

    // 1. Kiểm tra xem item đã được đánh giá chưa
    this.reviewService.getMyReviewedOrderItemIds().subscribe({
      next: (reviewedIds) => {
        if (reviewedIds && reviewedIds.includes(orderItemId)) {
          this.isAlreadyReviewed.set(true);
          this.isLoading.set(false);
          return;
        }

        // 2. Nếu chưa có orderItem trong state, fetch từ order list
        if (!this.orderItem()) {
          this.orderService.getMyOrders().subscribe({
            next: (orders) => {
              let foundItem: OrderItem | null = null;
              let foundOrder: Order | null = null;
              for (const ord of orders) {
                const it = ord.orderItems.find((i) => i.id === orderItemId);
                if (it) {
                  foundItem = it;
                  foundOrder = ord;
                  break;
                }
              }

              if (foundItem) {
                this.orderItem.set(foundItem);
                this.order.set(foundOrder);
                this.isLoading.set(false);
              } else {
                this.errorMessage.set('Không tìm thấy thông tin sản phẩm trong đơn hàng của bạn!');
                this.isLoading.set(false);
              }
            },
            error: () => {
              this.errorMessage.set('Không thể tải thông tin đơn hàng!');
              this.isLoading.set(false);
            },
          });
        } else {
          this.isLoading.set(false);
        }
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  setRating(stars: number): void {
    this.rating.set(stars);
  }

  setHoverRating(stars: number): void {
    this.hoverRating.set(stars);
  }

  appendTag(tag: string): void {
    const current = this.comment().trim();
    if (current.length === 0) {
      this.comment.set(tag);
    } else if (!current.includes(tag)) {
      this.comment.set(`${current}, ${tag}`);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.toastService.showError('Lỗi file', 'Vui lòng chỉ chọn tệp tin hình ảnh (PNG, JPG, WEBP)!');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.toastService.showError('File quá lớn', 'Kích thước ảnh tối đa cho phép là 10MB!');
      return;
    }

    this.isUploadingPhoto.set(true);

    // Gọi API upload hình ảnh lên Cloudinary
    this.reviewService.uploadImage(file).subscribe({
      next: (res) => {
        this.isUploadingPhoto.set(false);
        if (res && res.imageUrl) {
          this.photoUrl.set(res.imageUrl);
          this.toastService.showSuccess('Tải ảnh thành công', 'Ảnh chụp thực tế đã được tải lên Cloudinary!');
        }
      },
      error: (err) => {
        // Fallback: Nếu backend chưa config Cloudinary hoặc lỗi upload thì dùng Base64 preview
        const reader = new FileReader();
        reader.onload = (e) => {
          this.isUploadingPhoto.set(false);
          const base64 = e.target?.result as string;
          this.photoUrl.set(base64);
          this.toastService.showSuccess('Đã đính kèm ảnh', 'Ảnh chụp thực tế đã được đính kèm vào đánh giá!');
        };
        reader.onerror = () => {
          this.isUploadingPhoto.set(false);
          this.toastService.showError('Lỗi tải ảnh', 'Không thể đọc tệp tin hình ảnh!');
        };
        reader.readAsDataURL(file);
      },
    });
  }

  clearPhoto(): void {
    this.photoUrl.set('');
  }

  goBack(): void {
    this.router.navigate(['/profile/orders']);
  }

  submitReview(): void {
    const itemId = this.orderItemId();
    if (!itemId) return;

    const ratingVal = this.rating();
    const commentVal = this.comment().trim();
    const photoUrlVal = this.photoUrl().trim();

    this.isSubmitting.set(true);

    const photoUrls = photoUrlVal ? [photoUrlVal] : [];

    this.reviewService
      .createReview({
        orderItemId: itemId,
        rating: ratingVal,
        comment: commentVal,
        photoUrls: photoUrls,
      })
      .subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          const coins = res.coinsAwarded || 0;

          // Cập nhật ngay lập tức số dư Xu trong AuthService signal & nạp lại profile
          this.authService.currentUser.update((u) =>
            u ? { ...u, rewardCoins: (u.rewardCoins || 0) + coins } : null
          );
          this.authService.fetchProfile().subscribe();

          if (coins > 0) {
            this.toastService.showSuccess(
              'Đánh Giá Thành Công! 🎉',
              `Bạn nhận được +${coins} Xu thưởng vào ví tích lũy!`
            );
          } else {
            this.toastService.showSuccess(
              'Đánh Giá Thành Công!',
              'Cảm ơn bạn đã để lại đánh giá cho sản phẩm!'
            );
          }
          this.router.navigate(['/profile/orders']);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.toastService.showError(
            'Không thể gửi đánh giá',
            err?.error?.message || 'Có lỗi xảy ra, vui lòng thử lại sau!'
          );
        },
      });
  }
}
