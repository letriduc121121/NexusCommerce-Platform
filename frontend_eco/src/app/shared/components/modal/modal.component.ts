import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Trash2, AlertTriangle, CheckCircle2, ShoppingBag, Info, X, ArrowRight } from 'lucide-angular';

export type ModalType = 'confirm' | 'danger' | 'success' | 'info' | 'cart';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss'
})
export class ModalComponent {
  readonly TrashIcon = Trash2;
  readonly AlertIcon = AlertTriangle;
  readonly CheckIcon = CheckCircle2;
  readonly CartIcon = ShoppingBag;
  readonly InfoIcon = Info;
  readonly XIcon = X;
  readonly ArrowRightIcon = ArrowRight;

  @Input() isOpen = false;
  @Input() type: ModalType = 'confirm';
  @Input() title = '';
  @Input() message = '';
  @Input() confirmText = 'Xác nhận';
  @Input() cancelText = 'Hủy';
  @Input() isLoading = false;
  @Input() cartUrl = '/cart';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.handleCancel();
    }
  }

  handleConfirm(): void {
    if (!this.isLoading) {
      this.confirm.emit();
    }
  }

  handleCancel(): void {
    if (!this.isLoading) {
      this.cancel.emit();
      this.close.emit();
    }
  }
}
