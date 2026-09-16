import { Component, inject } from '@angular/core';
import { LucideAngularModule, CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-angular';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.scss'
})
export class ToastComponent {
  readonly toastService = inject(ToastService);

  readonly CheckIcon = CheckCircle2;
  readonly AlertIcon = AlertTriangle;
  readonly ErrorIcon = XCircle;
  readonly InfoIcon = Info;
  readonly XIcon = X;

  remove(id: number): void {
    this.toastService.remove(id);
  }
}
