import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Search, Pencil, RefreshCw, X } from 'lucide-angular';

import { InventoryService } from '../../../core/services/inventory.service';
import { StockResponse } from '../../../core/models';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

/** Dưới ngưỡng này thì cảnh báo sắp hết hàng để admin kịp nhập thêm. */
const NGUONG_SAP_HET = 10;

type StockFilter = 'ALL' | 'LOW' | 'OUT';

@Component({
  selector: 'app-inventory-management',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, PaginationComponent],
  templateUrl: './inventory-management.component.html',
  styleUrl: './inventory-management.component.scss'
})
export class InventoryManagementComponent implements OnInit {
  readonly SearchIcon = Search;
  readonly EditIcon = Pencil;
  readonly RefreshIcon = RefreshCw;
  readonly XIcon = X;
  readonly lowStock = NGUONG_SAP_HET;

  private readonly inventoryService = inject(InventoryService);

  items = signal<StockResponse[]>([]);
  keyword = signal('');
  stockFilter = signal<StockFilter>('ALL');
  loading = signal(false);
  success = signal('');
  error = signal('');

  page = signal(1);
  readonly pageSize = 10;

  // Modal cập nhật tồn kho
  editing = signal<StockResponse | null>(null);
  formQuantity = signal(0);

  filtered = computed(() => {
    const kw = this.keyword().toLowerCase().trim();
    const filter = this.stockFilter();

    return this.items().filter(i => {
      if (!i.active) return false;
      if (filter === 'OUT' && i.available > 0) return false;
      if (filter === 'LOW' && (i.available === 0 || i.available >= NGUONG_SAP_HET)) return false;
      if (!kw) return true;
      return [i.productName, i.productId].some(v => v?.toLowerCase().includes(kw));
    });
  });

  paged = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  /** Chênh lệch so với tồn hiện tại, để admin biết mình đang nhập thêm hay bớt đi. */
  delta = computed(() => this.formQuantity() - (this.editing()?.quantity ?? 0));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.inventoryService.getInventories().subscribe({
      next: list => {
        // Sắp xếp theo tên -> vị trí không đổi sau khi cập nhật tồn kho.
        this.items.set((list ?? []).filter(i => i.active)
          .sort((a, b) => (a.productName ?? '').localeCompare(b.productName ?? '')));
        this.loading.set(false);
      },
      error: err => this.fail(err, 'Không thể tải dữ liệu tồn kho.')
    });
  }

  filter(kw: string, filter: StockFilter): void {
    this.keyword.set(kw);
    this.stockFilter.set(filter);
    this.page.set(1);
  }

  openEdit(item: StockResponse): void {
    this.formQuantity.set(item.quantity);
    this.error.set('');
    this.editing.set(item);
  }

  save(): void {
    const item = this.editing();
    if (!item) return;

    const qty = this.formQuantity();
    // Tồn kho không được nhỏ hơn phần đang giữ chỗ, nếu không available sẽ âm.
    if (qty < item.reserved) {
      this.error.set(`Số lượng phải >= ${item.reserved} (đang giữ chỗ cho các đơn chưa chốt).`);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.inventoryService.updateQuantity(item.productId, qty).subscribe({
      next: () => {
        this.editing.set(null);
        this.loading.set(false);
        this.success.set(`Đã cập nhật tồn kho "${item.productName}" thành ${qty}.`);
        setTimeout(() => this.success.set(''), 3000);
        this.load();
      },
      error: err => this.fail(err, 'Cập nhật số lượng thất bại.')
    });
  }

  stockClass(item: StockResponse): string {
    if (item.available === 0) return 'badge badge-danger';
    if (item.available < NGUONG_SAP_HET) return 'badge badge-warning';
    return 'badge badge-success';
  }

  private fail(err: any, fallback: string): void {
    this.loading.set(false);
    this.error.set(err?.error?.message || fallback);
  }
}
