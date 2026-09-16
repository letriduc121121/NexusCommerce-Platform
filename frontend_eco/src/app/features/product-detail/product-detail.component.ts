import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule, ChevronDown, ShoppingBag, Check } from 'lucide-angular';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Product } from '../../core/models';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, LucideAngularModule],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss'
})
export class ProductDetailComponent {
  readonly ChevronDownIcon = ChevronDown;
  readonly ShoppingBagIcon = ShoppingBag;
  readonly CheckIcon = Check;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  product = signal<Product | null>(null);
  related = signal<Product[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');

  quantity = signal(1);
  isAdding = signal(false);
  isCartModalOpen = signal(false);

  /** Accordion nào đang mở. */
  openPanel = signal<string>('details');

  constructor() {
    // paramMap là stream nên khi bấm sang sản phẩm liên quan, trang tự nạp lại.
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) this.loadProduct(id);
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
      }
    });
  }

  /** API theo danh mục không tự lọc hàng ngừng kinh doanh nên phải lọc ở đây. */
  private loadRelated(categoryId: number, currentId: string): void {
    this.productService.getProductsByCategory(categoryId).subscribe({
      next: (list) => this.related.set(
        (list || [])
          .filter(p => p.id !== currentId && p.status !== 'DISCONTINUED')
          .slice(0, 4)
      ),
      error: () => this.related.set([])
    });
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
      this.toastService.showWarning('Sản phẩm không khả dụng', 'Sản phẩm này hiện không có sẵn');
      return;
    }

    this.isAdding.set(true);

    this.cartService.addItem({
      productId: p.id, quantity: this.quantity(),
      productName: p.name, price: p.price, imageUrl: p.imageUrl,
    }).subscribe({
      next: () => {
        this.isAdding.set(false);
        this.toastService.showSuccess('Thành công', `${p.name} đã được thêm vào giỏ hàng (x${this.quantity()})`);
        this.quantity.set(1);
      },
      error: (err) => {
        this.isAdding.set(false);
        const errorMsg = err?.error?.message || 'Lỗi khi thêm vào giỏ hàng. Vui lòng thử lại.';
        const lowerMsg = errorMsg.toLowerCase();

        if (lowerMsg.includes('hết') || lowerMsg.includes('out of stock') || lowerMsg.includes('không đủ')) {
          this.toastService.showWarning('Sản phẩm hết hàng', 'Sản phẩm này hiện đã hết hàng');
        } else {
          this.toastService.showError('Lỗi', errorMsg);
        }
      }
    });
  }
}
