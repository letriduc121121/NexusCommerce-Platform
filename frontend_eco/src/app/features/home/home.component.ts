import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  ArrowRight,
  ShieldCheck,
  Truck,
  RefreshCw,
  Headphones,
  ShoppingBag,
  CreditCard,
  Search,
  X,
  SlidersHorizontal,
  ArrowUpDown,
  Copy,
  Ticket
} from 'lucide-angular';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { VoucherService } from '../../core/services/voucher.service';
import { DISCOUNT_TYPE_LABEL, Product, Voucher } from '../../core/models';
import { PaginationComponent } from '../../shared/components/pagination/pagination.component';

export interface PriceRangeOption {
  label: string;
  min: number | null;
  max: number | null;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, FormsModule, LucideAngularModule, PaginationComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  readonly ArrowRightIcon = ArrowRight;
  readonly ShieldCheckIcon = ShieldCheck;
  readonly TruckIcon = Truck;
  readonly RefreshCwIcon = RefreshCw;
  readonly HeadphonesIcon = Headphones;
  readonly ShoppingBagIcon = ShoppingBag;
  readonly SearchIcon = Search;
  readonly XIcon = X;
  readonly SlidersIcon = SlidersHorizontal;
  readonly SortIcon = ArrowUpDown;
  readonly CopyIcon = Copy;
  readonly TicketIcon = Ticket;
  readonly discountLabel = DISCOUNT_TYPE_LABEL;

  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private voucherService = inject(VoucherService);
  private router = inject(Router);

  allProducts = signal<Product[]>([]);
  isLoadingProducts = signal(true);

  vouchers = signal<Voucher[]>([]);

  searchQuery = signal('');
  selectedCategoryId = signal<number | null>(null);
  selectedPriceRange = signal<PriceRangeOption>({ label: 'Tất cả giá', min: null, max: null });
  sortOption = signal<'default' | 'price-asc' | 'price-desc'>('default');

  currentPage = signal(1);
  pageSize = signal(12);
  addingProductId = signal<string | null>(null);

  readonly priceRanges: PriceRangeOption[] = [
    { label: 'Tất cả giá', min: null, max: null },
    { label: 'Dưới 2 triệu', min: 0, max: 2000000 },
    { label: '2 – 5 triệu', min: 2000000, max: 5000000 },
    { label: '5 – 10 triệu', min: 5000000, max: 10000000 },
    { label: '10 – 20 triệu', min: 10000000, max: 20000000 },
    { label: 'Trên 20 triệu', min: 20000000, max: null }
  ];

  features = signal([
    {
      icon: ShieldCheck,
      title: 'Bảo Hành Chính Hãng',
      desc: 'Cam kết 100% linh kiện & thiết bị chính hãng'
    },
    {
      icon: Truck,
      title: 'Giao Hàng Hỏa Tốc',
      desc: 'Miễn phí vận chuyển toàn quốc cho đơn từ 2.000.000đ'
    },
    {
      icon: CreditCard,
      title: 'Trả Góp 0% Lãi Suất',
      desc: 'Thủ tục duyệt nhanh trong 5 phút qua thẻ tín dụng'
    },
    {
      icon: Headphones,
      title: 'Hỗ Trợ Kỹ Thuật 24/7',
      desc: 'Đội ngũ chuyên gia tech tư vấn tận tâm'
    }
  ]);

  filteredAllProducts = computed(() => {
    let list = this.allProducts().filter(p => p.status === 'ACTIVE');
    const query = this.searchQuery().trim().toLowerCase();
    const categoryId = this.selectedCategoryId();
    const range = this.selectedPriceRange();
    const sort = this.sortOption();

    if (query) {
      list = list.filter(p =>
        p.name.toLowerCase().includes(query) ||
        (p.category?.name && p.category.name.toLowerCase().includes(query)) ||
        (p.description && p.description.toLowerCase().includes(query))
      );
    }

    if (categoryId) {
      list = list.filter(p => p.category?.id === categoryId);
    }

    if (range.min !== null) {
      list = list.filter(p => p.price >= range.min!);
    }
    if (range.max !== null) {
      list = list.filter(p => p.price <= range.max!);
    }

    if (sort === 'price-asc') {
      list = [...list].sort((a, b) => a.price - b.price);
    } else if (sort === 'price-desc') {
      list = [...list].sort((a, b) => b.price - a.price);
    }

    return list;
  });

  totalProducts = computed(() => this.filteredAllProducts().length);

  filteredProducts = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.filteredAllProducts().slice(start, start + this.pageSize());
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.totalProducts() / this.pageSize())));

  hasActiveFilters = computed(() => {
    return !!this.searchQuery() || this.selectedCategoryId() !== null || this.selectedPriceRange().min !== null || this.selectedPriceRange().max !== null || this.sortOption() !== 'default';
  });

  categories = computed(() => {
    const cats: { [key: number]: any } = {};
    this.allProducts().forEach(p => {
      if (p.category && !cats[p.category.id]) {
        cats[p.category.id] = p.category;
      }
    });
    return Object.values(cats);
  });

  ngOnInit(): void {
    this.isLoadingProducts.set(true);
    this.productService.getProducts().subscribe({
      next: (list) => {
        this.allProducts.set(list || []);
        this.isLoadingProducts.set(false);
      },
      error: () => {
        this.allProducts.set([]);
        this.isLoadingProducts.set(false);
      }
    });

    this.voucherService.available().subscribe({
      next: list => this.vouchers.set(list ?? []),
      error: () => this.vouchers.set([]),
    });
  }

  /** Copy mã voucher vào clipboard. */
  copyCode(code: string): void {
    navigator.clipboard.writeText(code).then(
      () => this.toastService.showSuccess('Đã copy', `Mã "${code}" — dán ở trang thanh toán để dùng`),
      () => this.toastService.showError('Lỗi', 'Không copy được mã'),
    );
  }

  onSearchChange(query: string): void {
    this.searchQuery.set(query);
    this.currentPage.set(1);
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.currentPage.set(1);
  }

  selectCategory(categoryId: number | null): void {
    this.selectedCategoryId.set(categoryId);
    this.currentPage.set(1);
  }

  selectPriceRange(option: PriceRangeOption): void {
    this.selectedPriceRange.set(option);
    this.currentPage.set(1);
  }

  setSortOption(option: 'default' | 'price-asc' | 'price-desc'): void {
    this.sortOption.set(option);
    this.currentPage.set(1);
  }

  resetAllFilters(): void {
    this.searchQuery.set('');
    this.selectedCategoryId.set(null);
    this.selectedPriceRange.set(this.priceRanges[0]);
    this.sortOption.set('default');
    this.currentPage.set(1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  addToCart(product: Product, event?: Event): void {
    if (event) event.stopPropagation();

    if (product.status !== 'ACTIVE') {
      this.toastService.showWarning('Sản phẩm không khả dụng', 'Sản phẩm này hiện không có sẵn');
      return;
    }

    this.addingProductId.set(product.id);

    this.cartService.addItem({
      productId: product.id, quantity: 1,
      productName: product.name, price: product.price, imageUrl: product.imageUrl,
    }).subscribe({
      next: () => {
        this.addingProductId.set(null);
        this.toastService.showSuccess('Thành công', `${product.name} đã được thêm vào giỏ hàng`);
      },
      error: (err) => {
        this.addingProductId.set(null);
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



