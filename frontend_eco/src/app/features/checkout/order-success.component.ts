import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';

import { Order } from '../../core/models';

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CurrencyPipe],
  template: `
    <div class="success-page">
      <div class="icon">✓</div>
      <h1>Đặt Hàng Thành Công</h1>

      @if (order(); as o) {
        <div class="summary">
          <div class="row"><span>Mã đơn</span><span>#{{ o.id.slice(0, 8).toUpperCase() }}</span></div>
          <div class="row"><span>Tạm tính</span><span>{{ (o.subtotal ?? o.totalAmount) | currency:'VND':'symbol-narrow':'1.0-0' }}</span></div>
          @if (o.discountAmount && o.discountAmount > 0) {
            <div class="row discount">
              <span>Voucher{{ o.voucherCode ? ' (' + o.voucherCode + ')' : '' }}</span>
              <span>−{{ o.discountAmount | currency:'VND':'symbol-narrow':'1.0-0' }}</span>
            </div>
          }
          <div class="row total"><span>Tổng thanh toán</span><span>{{ o.totalAmount | currency:'VND':'symbol-narrow':'1.0-0' }}</span></div>
        </div>
      }

      <p>Tự động chuyển về trang chủ trong <strong>{{ countdown() }}s</strong>...</p>
    </div>
  `,
  styles: [`
    .success-page { min-height: 70vh; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 20px; }
    .icon { width: 64px; height: 64px; background: #10b981; color: #fff; font-size: 32px; font-weight: bold; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 16px; }
    h1 { font-size: 24px; font-weight: 700; color: #111827; margin: 0 0 16px; }
    p { font-size: 15px; color: #6b7280; margin: 16px 0 0; }
    .summary { width: 100%; max-width: 320px; border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px 16px; font-size: 0.88rem; }
    .summary .row { display: flex; justify-content: space-between; padding: 4px 0; color: #475569; }
    .summary .discount { color: #b91c1c; }
    .summary .total { border-top: 1px dashed #cbd5e1; margin-top: 6px; padding-top: 8px; font-weight: 700; color: #0f172a; }
  `]
})
export class OrderSuccessComponent implements OnInit, OnDestroy {
  private router = inject(Router);

  countdown = signal(5);
  order = signal<Order | null>(
    (this.router.getCurrentNavigation()?.extras.state?.['order'] ?? history.state?.['order']) as Order | null
  );

  private timer: any;

  ngOnInit(): void {
    this.timer = setInterval(() => {
      if (this.countdown() <= 1) {
        clearInterval(this.timer);
        this.router.navigate(['/']);
      } else {
        this.countdown.update(c => c - 1);
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }
}
