import { Injectable, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { OrderNotification } from '../models';

/** SockJS + StompJS nạp từ CDN trong index.html nên khai báo kiểu thủ công. */
declare const SockJS: any;
declare const Stomp: any;

const TOPIC = '/topic/admin/orders';
const RETRY_DELAY = 5000;

/**
 * Lớp vận chuyển thuần: giữ một kết nối STOMP tới order-service và phát lại message nhận được.
 * Không chứa logic nghiệp vụ - phần đó nằm ở AdminNotificationService.
 */
@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private client: any = null;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  /** Phân biệt "rớt mạng, cần nối lại" với "chủ động ngắt, đừng nối lại nữa". */
  private wanted = false;

  private readonly notifications = new Subject<OrderNotification>();

  /** Cho giao diện hiện chấm xanh "đang đồng bộ" / xám "mất kết nối". */
  readonly connected = signal(false);

  get notifications$(): Observable<OrderNotification> {
    return this.notifications.asObservable();
  }

  connect(): void {
    this.wanted = true;
    if (this.client) return;

    this.client = Stomp.over(new SockJS(environment.wsOrdersUrl));
    this.client.debug = () => {}; // Tắt log mặc định rất ồn của StompJS.

    this.client.connect(
      {},
      () => {
        this.connected.set(true);
        this.client.subscribe(TOPIC, (frame: any) => this.notifications.next(JSON.parse(frame.body)));
      },
      () => this.scheduleRetry()
    );
  }

  disconnect(): void {
    this.wanted = false;
    this.connected.set(false);
    this.clearRetry();

    try {
      this.client?.disconnect();
    } catch {
      // Ngắt lúc đang bắt tay dở có thể ném lỗi - không ảnh hưởng gì.
    }
    this.client = null;
  }

  /** Order-service khởi động lại hoặc mạng rớt -> tự nối lại, Admin không phải F5. */
  private scheduleRetry(): void {
    this.connected.set(false);
    this.client = null;
    if (!this.wanted || this.retryTimer) return;

    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.connect();
    }, RETRY_DELAY);
  }

  private clearRetry(): void {
    if (this.retryTimer) clearTimeout(this.retryTimer);
    this.retryTimer = null;
  }
}
