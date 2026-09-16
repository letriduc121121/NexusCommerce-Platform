import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: number;
  type: ToastType;
  title: string;
  message: string;
  duration?: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  toasts = signal<ToastMessage[]>([]);

  show(type: ToastType, title: string, message: string, duration = 3500): void {
    const id = this.nextId++;
    const newToast: ToastMessage = { id, type, title, message, duration };

    this.toasts.update(list => [...list, newToast]);

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }
  }

  showSuccess(title = 'Thành công', message = 'Thao tác thành công!'): void {
    this.show('success', title, message);
  }

  showError(title = 'Lỗi', message = 'Đã có lỗi xảy ra!'): void {
    this.show('error', title, message);
  }

  showWarning(title = 'Cảnh báo', message = 'Cảnh báo hệ thống!'): void {
    this.show('warning', title, message);
  }

  showInfo(title = 'Thông tin', message = 'Thông tin hệ thống!'): void {
    this.show('info', title, message);
  }

  remove(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
