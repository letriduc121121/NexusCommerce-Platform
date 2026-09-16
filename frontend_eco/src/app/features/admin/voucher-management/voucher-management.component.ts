import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Pause, Pencil, Play, Plus, Search, Trash2, X } from 'lucide-angular';

import { VoucherService } from '../../../core/services/voucher.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import {
  DISCOUNT_TYPE_LABEL,
  VOUCHER_STATUS_LABEL,
  Voucher,
  VoucherRequest,
  VoucherStatus,
} from '../../../core/models';

type Tab = 'ALL' | 'ACTIVE' | 'ENDED';

const EMPTY_FORM: VoucherRequest = {
  code: '',
  name: '',
  description: '',
  discountType: 'FIXED_AMOUNT',
  discountValue: 0,
  maxDiscountAmount: null,
  minOrderAmount: 0,
  totalQuantity: null,
  usableFrom: '',
  usableTo: '',
};

@Component({
  selector: 'app-voucher-management',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe, LucideAngularModule, PaginationComponent],
  templateUrl: './voucher-management.component.html',
  styleUrl: './voucher-management.component.scss',
})
export class VoucherManagementComponent implements OnInit {
  readonly PlusIcon = Plus;
  readonly EditIcon = Pencil;
  readonly DeleteIcon = Trash2;
  readonly SearchIcon = Search;
  readonly XIcon = X;
  readonly PauseIcon = Pause;
  readonly PlayIcon = Play;

  readonly typeLabel = DISCOUNT_TYPE_LABEL;
  readonly statusLabel = VOUCHER_STATUS_LABEL;

  private readonly voucherService = inject(VoucherService);

  vouchers = signal<Voucher[]>([]);
  keyword = signal('');
  tab = signal<Tab>('ALL');
  loading = signal(false);
  success = signal('');
  error = signal('');

  page = signal(1);
  readonly pageSize = 10;

  /** null = modal đóng. 0 = tạo mới. >0 = sửa voucher id đó. */
  modalId = signal<number | null>(null);
  form = signal<VoucherRequest>({ ...EMPTY_FORM });

  filtered = computed(() => {
    const kw = this.keyword().toLowerCase().trim();
    const tab = this.tab();
    return this.vouchers().filter(v => {
      if (tab === 'ACTIVE' && v.status !== 'ACTIVE') return false;
      if (tab === 'ENDED' && v.status !== 'ENDED') return false;
      if (!kw) return true;
      return [v.code, v.name].some(s => s.toLowerCase().includes(kw));
    });
  });

  paged = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.voucherService.getAll().subscribe({
      next: list => {
        // Sắp xếp cố định theo id -> vị trí dòng không đổi sau khi sửa/đổi trạng thái.
        this.vouchers.set([...(list ?? [])].sort((a, b) => a.id - b.id));
        this.loading.set(false);
      },
      error: err => this.fail(err, 'Không tải được danh sách voucher.'),
    });
  }

  openCreate(): void {
    this.form.set({ ...EMPTY_FORM });
    this.error.set('');
    this.modalId.set(0);
  }

  openEdit(v: Voucher): void {
    this.form.set({
      code: v.code,
      name: v.name,
      description: v.description ?? '',
      discountType: v.discountType,
      discountValue: v.discountValue,
      maxDiscountAmount: v.maxDiscountAmount ?? null,
      minOrderAmount: v.minOrderAmount,
      totalQuantity: v.totalQuantity ?? null,
      usableFrom: v.usableFrom.slice(0, 16),
      usableTo: v.usableTo.slice(0, 16),
    });
    this.error.set('');
    this.modalId.set(v.id);
  }

  closeModal(): void {
    this.modalId.set(null);
  }

  /** Cập nhật 1 field trong form (giữ signal bất biến). */
  patch<K extends keyof VoucherRequest>(key: K, value: VoucherRequest[K]): void {
    this.form.update(f => ({ ...f, [key]: value }));
  }

  save(): void {
    const id = this.modalId();
    if (id === null) return;

    this.loading.set(true);
    this.error.set('');

    const body = this.form();
    const req = id === 0
      ? this.voucherService.create(body)
      : this.voucherService.update(id, body);

    req.subscribe({
      next: () => {
        this.closeModal();
        this.notify(id === 0 ? 'Đã tạo voucher.' : 'Đã cập nhật voucher.');
        this.load();
      },
      error: err => this.fail(err, 'Lưu voucher thất bại.'),
    });
  }

  /** Đổi bộ lọc -> quay về trang 1. */
  onKeyword(kw: string): void {
    this.keyword.set(kw);
    this.page.set(1);
  }

  onTab(t: Tab): void {
    this.tab.set(t);
    this.page.set(1);
  }

  /** Tạm dừng <-> kích hoạt voucher. */
  toggleStatus(v: Voucher): void {
    const next: VoucherStatus = v.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    this.loading.set(true);
    this.error.set('');
    this.voucherService.setStatus(v.id, next).subscribe({
      next: () => {
        this.notify(next === 'PAUSED' ? `Đã tạm dừng "${v.code}".` : `Đã kích hoạt "${v.code}".`);
        this.load();
      },
      error: err => this.fail(err, 'Đổi trạng thái thất bại.'),
    });
  }

  remove(v: Voucher): void {
    if (!confirm(`Xoá voucher "${v.code}"?`)) return;
    this.loading.set(true);
    this.error.set('');
    this.voucherService.delete(v.id).subscribe({
      next: () => {
        this.notify(`Đã xoá "${v.code}".`);
        this.load();
      },
      error: err => this.fail(err, 'Xoá thất bại.'),
    });
  }

  statusClass(s: VoucherStatus): string {
    if (s === 'ACTIVE') return 'badge badge-success';
    if (s === 'PAUSED') return 'badge badge-warning';
    return 'badge badge-danger';
  }

  private notify(msg: string): void {
    this.loading.set(false);
    this.success.set(msg);
    setTimeout(() => this.success.set(''), 3000);
  }

  private fail(err: any, fallback: string): void {
    this.loading.set(false);
    this.error.set(err?.error?.message || fallback);
  }
}
