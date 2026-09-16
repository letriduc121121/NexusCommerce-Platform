import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { LucideAngularModule, ChevronLeft, ChevronRight } from 'lucide-angular';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    @if (totalPages > 1) {
      <div class="pagination-container">
        <span class="pagination-info">
          Hiển thị {{ startIndex + 1 }} - {{ endIndex }} trong số {{ totalItems }} kết quả
        </span>

        <div class="pagination-controls">
          <button
            class="page-btn nav-btn"
            (click)="selectPage(currentPage - 1)"
            [disabled]="currentPage <= 1"
            title="Trang trước"
          >
            <lucide-angular [img]="ChevronLeftIcon" [size]="16"></lucide-angular>
          </button>

          @for (page of pagesList; track $index) {
            @if (page === -1) {
              <span class="page-dots">...</span>
            } @else {
              <button
                class="page-btn"
                [class.active]="page === currentPage"
                (click)="selectPage(page)"
              >
                {{ page }}
              </button>
            }
          }

          <button
            class="page-btn nav-btn"
            (click)="selectPage(currentPage + 1)"
            [disabled]="currentPage >= totalPages"
            title="Trang tiếp"
          >
            <lucide-angular [img]="ChevronRightIcon" [size]="16"></lucide-angular>
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    .pagination-container {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid #e2e8f0;
      flex-wrap: wrap;
    }
    .pagination-info {
      font-size: 0.85rem;
      color: #64748b;
    }
    .pagination-controls {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .page-btn {
      min-width: 34px;
      height: 34px;
      padding: 0 8px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
      font-weight: 500;
      border: 1px solid #cbd5e1;
      background: #ffffff;
      color: #334155;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover:not(:disabled) {
        border-color: #0f172a;
        color: #0f172a;
        background: #f8fafc;
      }
      &.active {
        background: #0f172a;
        color: #ffffff;
        border-color: #0f172a;
      }
      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    }
    .page-dots {
      padding: 0 4px;
      color: #94a3b8;
    }
  `]
})
export class PaginationComponent implements OnChanges {
  readonly ChevronLeftIcon = ChevronLeft;
  readonly ChevronRightIcon = ChevronRight;

  @Input() currentPage = 1;
  @Input() pageSize = 8;
  @Input() totalItems = 0;

  @Output() pageChange = new EventEmitter<number>();

  totalPages = 1;
  startIndex = 0;
  endIndex = 0;
  pagesList: number[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    this.calculate();
  }

  calculate(): void {
    this.totalPages = Math.ceil(this.totalItems / this.pageSize) || 1;
    this.startIndex = (this.currentPage - 1) * this.pageSize;
    this.endIndex = Math.min(this.startIndex + this.pageSize, this.totalItems);
    this.pagesList = this.getVisiblePages();
  }

  getVisiblePages(): number[] {
    const total = this.totalPages;
    const current = this.currentPage;

    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }

    if (current <= 4) {
      return [1, 2, 3, 4, 5, -1, total];
    }

    if (current >= total - 3) {
      return [1, -1, total - 4, total - 3, total - 2, total - 1, total];
    }

    return [1, -1, current - 1, current, current + 1, -1, total];
  }

  selectPage(page: number): void {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }
}
