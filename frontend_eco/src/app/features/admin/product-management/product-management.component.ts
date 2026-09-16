import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Plus, Search, Edit2, Trash2, RefreshCw, X, Upload } from 'lucide-angular';
import { ProductService } from '../../../core/services/product.service';
import { Category, Product, ProductRequest } from '../../../core/models';

import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

/** Giá trị rỗng dùng cho form thêm mới. */
const EMPTY_FORM: ProductRequest = {
  category: { id: 0 },
  sku: '',
  name: '',
  description: '',
  price: 0,
  imageUrl: '',
  status: 'ACTIVE'
};

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CurrencyPipe, FormsModule, LucideAngularModule, PaginationComponent, ModalComponent],
  templateUrl: './product-management.component.html',
  styleUrl: './product-management.component.scss'
})
export class ProductManagementComponent implements OnInit {
  readonly PlusIcon = Plus;
  readonly SearchIcon = Search;
  readonly EditIcon = Edit2;
  readonly TrashIcon = Trash2;
  readonly RefreshIcon = RefreshCw;
  readonly XIcon = X;
  readonly UploadIcon = Upload;

  private productService = inject(ProductService);

  products = signal<Product[]>([]);
  categories = signal<Category[]>([]);
  searchQuery = signal('');
  isLoading = signal(false);
  successMessage = signal('');
  errorMessage = signal('');

  currentPage = signal(1);
  pageSize = 10;

  isModalOpen = signal(false);
  /** null = đang thêm mới, có giá trị = đang sửa. */
  editingId = signal<string | null>(null);
  form = signal<ProductRequest>({ ...EMPTY_FORM });

  filteredProducts = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.products();
    return this.products().filter(p =>
      p.name.toLowerCase().includes(query) || p.sku.toLowerCase().includes(query)
    );
  });

  paginatedProducts = computed(() => {
    const list = this.filteredProducts();
    const start = (this.currentPage() - 1) * this.pageSize;
    return list.slice(start, start + this.pageSize);
  });

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.currentPage.set(1);
  }

  ngOnInit(): void {
    this.loadProducts();
    this.productService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats || []),
      error: () => this.categories.set([])
    });
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.productService.getProducts().subscribe({
      next: (list) => {
        // Sắp xếp cố định (mới nhất trên đầu) -> vị trí không đổi sau khi sửa.
        this.products.set([...(list || [])].sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '')));
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Không tải được danh sách sản phẩm.');
      }
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.set({ ...EMPTY_FORM });
    this.errorMessage.set('');
    this.isModalOpen.set(true);
  }

  openEdit(p: Product): void {
    this.editingId.set(p.id);
    this.form.set({
      category: { id: p.category?.id || 0 },
      sku: p.sku,
      name: p.name,
      description: p.description || '',
      price: p.price,
      imageUrl: p.imageUrl || '',
      status: p.status || 'ACTIVE'
    });
    this.errorMessage.set('');
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.editingId.set(null);
  }

  /** Cập nhật một field của form (signal giữ object nên phải clone). */
  setField(key: keyof ProductRequest, value: unknown): void {
    this.form.set({ ...this.form(), [key]: value } as ProductRequest);
  }

  setCategory(id: number): void {
    this.form.set({ ...this.form(), category: { id: Number(id) } });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = (e: ProgressEvent<FileReader>) => {
        const base64 = e.target?.result as string;
        this.setField('imageUrl', base64);
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(): void {
    this.setField('imageUrl', '');
  }

  submit(): void {
    const data = this.form();
    if (!data.name || !data.sku || !data.category.id || data.price <= 0) {
      this.errorMessage.set('Vui lòng nhập đủ Tên, SKU, Danh mục và Giá lớn hơn 0.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const id = this.editingId();
    const request$ = id
      ? this.productService.updateProduct(id, data)
      : this.productService.createProduct(data);

    request$.subscribe({
      next: () => {
        this.isLoading.set(false);
        this.closeModal();
        this.showSuccess(id ? 'Cập nhật sản phẩm thành công!' : 'Thêm sản phẩm mới thành công!');
        this.loadProducts();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Lưu sản phẩm thất bại.');
      }
    });
  }

  // Delete modal state
  deleteTarget = signal<Product | null>(null);
  isDeleteModalOpen = signal(false);

  openDeleteModal(p: Product): void {
    this.deleteTarget.set(p);
    this.isDeleteModalOpen.set(true);
  }

  confirmDelete(): void {
    const p = this.deleteTarget();
    if (!p) return;

    this.isLoading.set(true);
    this.productService.deleteProduct(p.id).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isDeleteModalOpen.set(false);
        this.deleteTarget.set(null);
        this.showSuccess('Đã ngừng kinh doanh sản phẩm thành công!');
        this.loadProducts();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Không thể xóa sản phẩm.');
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(''), 3000);
  }
}
