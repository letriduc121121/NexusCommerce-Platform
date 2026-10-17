import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  ChevronDown,
  ShoppingBag,
  Check,
  Star,
  Camera,
  Coins,
  ShieldCheck,
  X,
  MessageSquare,
  Sparkles,
} from 'lucide-angular';
import { DialogModule } from 'primeng/dialog';
import { RatingModule } from 'primeng/rating';
import { ProductService } from '../../core/services/product.service';
import { ReviewService } from '../../core/services/review.service';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Product, ReviewResponse, VerifyPurchaseResponse } from '../../core/models';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    LucideAngularModule,
    DialogModule,
    RatingModule,
  ],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
})
export class ProductDetailComponent {
  readonly ChevronDownIcon = ChevronDown;
  readonly ShoppingBagIcon = ShoppingBag;
  readonly CheckIcon = Check;
  readonly StarIcon = Star;
  readonly CameraIcon = Camera;
  readonly CoinsIcon = Coins;
  readonly ShieldCheckIcon = ShieldCheck;
  readonly XIcon = X;
  readonly MessageSquareIcon = MessageSquare;
  readonly SparklesIcon = Sparkles;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private reviewService = inject(ReviewService);
  private cartService = inject(CartService);
  public authService = inject(AuthService);
  private toastService = inject(ToastService);

  product = signal<Product | null>(null);
  related = signal<Product[]>([]);
  reviews = signal<ReviewResponse[]>([]);
  verifyInfo = signal<VerifyPurchaseResponse | null>(null);

  isLoading = signal(true);
  errorMessage = signal('');

  quantity = signal(1);
  isAdding = signal(false);
  openPanel = signal<string>('details');

  // Lọc đánh giá (0 = Tất cả, 1-5 sao)
  ratingFilter = signal<number>(0);

  // Review Modal State
  showReviewModal = signal(false);
  reviewRating = signal(5);
  reviewComment = signal('');
  reviewImages = signal<string[]>([]);
  isUploadingImage = signal(false);
  isSubmittingReview = signal(false);

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.loadProduct(id);
        this.loadReviews(id);
      }
    });
  }

  loadProduct(id: string): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.product.set(null);
    this.related.set([]);
    window.scrollTo({ top: 0 });

    this.productService.getProductById(id).subscribe({
      next: (p) => {
        this.product.set(p);
        this.isLoading.set(false);
        if (p.category?.id) this.loadRelated(p.category.id, p.id);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Không tìm thấy sản phẩm này.');
      },
    });
  }

  loadReviews(productId: string): void {
    this.reviewService.getReviewsByProduct(productId).subscribe({
      next: (data) => this.reviews.set(data || []),
      error: () => this.reviews.set([]),
    });
  }

  private loadRelated(categoryId: number, currentId: string): void {
    this.productService.getProductsByCategory(categoryId).subscribe({
      next: (list) =>
        this.related.set(
          (list || [])
            .filter((p) => p.id !== currentId && p.status !== 'DISCONTINUED')
            .slice(0, 4)
        ),
      error: () => this.related.set([]),
    });
  }

  filteredReviews(): ReviewResponse[] {
    const filter = this.ratingFilter();
    const list = this.reviews();
    if (filter === 0) return list;
    return list.filter((r) => r.rating === filter);
  }

  togglePanel(name: string): void {
    this.openPanel.set(this.openPanel() === name ? '' : name);
  }

  changeQuantity(delta: number): void {
    const next = this.quantity() + delta;
    if (next >= 1) this.quantity.set(next);
  }

  addToCart(): void {
    const p = this.product();
    if (!p) return;

    if (p.status !== 'ACTIVE') {
      this.toastService.showWarning(
        'Sản phẩm không khả dụng',
        'Sản phẩm này hiện không có sẵn'
      );
      return;
    }

    this.isAdding.set(true);

    this.cartService
      .addItem({
        productId: p.id,
        quantity: this.quantity(),
        productName: p.name,
        price: p.price,
        imageUrl: p.imageUrl,
      })
      .subscribe({
        next: () => {
          this.isAdding.set(false);
          this.toastService.showSuccess(
            'Thành công',
            `${p.name} đã được thêm vào giỏ hàng (x${this.quantity()})`
          );
          this.quantity.set(1);
        },
        error: (err) => {
          this.isAdding.set(false);
          const errorMsg =
            err?.error?.message || 'Lỗi khi thêm vào giỏ hàng. Vui lòng thử lại.';
          this.toastService.showError('Lỗi', errorMsg);
        },
      });
  }

  // --- Modal Đánh Giá Thưởng Xu ---

  openReviewModal(): void {
    if (!this.authService.isLoggedIn()) {
      this.toastService.showWarning('Yêu cầu đăng nhập', 'Vui lòng đăng nhập để viết đánh giá.');
      this.router.navigate(['/login']);
      return;
    }
    this.toastService.showInfo(
      'Viết Đánh Giá Tích Xu',
      'Vui lòng mở Lịch sử đơn hàng và chọn sản phẩm thuộc đơn ĐÃ GIAO để viết đánh giá nhận đến 200 Xu thưởng!'
    );
    this.router.navigate(['/profile']);
  }
}
