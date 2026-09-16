import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, KeyRound, Lock, Eye, EyeOff, Save, CheckCircle, AlertCircle, Check, X } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';

/** Khớp với @Pattern của ChangePasswordRequest ở auth_service. */
const NEW_PASSWORD_PATTERN = /^[A-Z](?=.*[0-9])(?=.*[@#$%^&+=!._-]).{5,}$/;

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [FormsModule, LucideAngularModule],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.scss'
})
export class ChangePasswordComponent {
  readonly KeyRoundIcon = KeyRound;
  readonly LockIcon = Lock;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly SaveIcon = Save;
  readonly CheckCircleIcon = CheckCircle;
  readonly AlertCircleIcon = AlertCircle;
  readonly CheckIcon = Check;
  readonly XIcon = X;

  private authService = inject(AuthService);

  oldPassword = signal('');
  newPassword = signal('');
  confirmPassword = signal('');

  showOld = signal(false);
  showNew = signal(false);
  showConfirm = signal(false);

  submitting = signal(false);
  successMessage = signal('');
  errorMessage = signal('');

  /** Các quy tắc soi theo đúng vị trí mà regex backend kiểm tra. */
  rules = computed(() => {
    const pwd = this.newPassword();
    const rest = pwd.slice(1);
    return [
      { label: 'Bắt đầu bằng một chữ cái viết hoa (A-Z)', ok: /^[A-Z]/.test(pwd) },
      { label: 'Chứa ít nhất một chữ số', ok: /[0-9]/.test(rest) },
      { label: 'Chứa ít nhất một ký tự đặc biệt (@ # $ % ^ & + = ! . _ -)', ok: /[@#$%^&+=!._-]/.test(rest) },
      { label: 'Độ dài tối thiểu 6 ký tự', ok: pwd.length >= 6 }
    ];
  });

  newPasswordValid = computed(() => NEW_PASSWORD_PATTERN.test(this.newPassword()));

  confirmMismatch = computed(
    () => this.confirmPassword().length > 0 && this.confirmPassword() !== this.newPassword()
  );

  sameAsOld = computed(
    () => this.newPassword().length > 0 && this.newPassword() === this.oldPassword()
  );

  canSubmit = computed(
    () =>
      !this.submitting() &&
      this.oldPassword().length > 0 &&
      this.newPasswordValid() &&
      !this.confirmMismatch() &&
      this.confirmPassword().length > 0 &&
      !this.sameAsOld()
  );

  onSubmit(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (!this.canSubmit()) {
      this.errorMessage.set('Vui lòng điền đầy đủ và đúng định dạng các trường bên dưới.');
      return;
    }

    this.submitting.set(true);
    this.authService.changePassword({
      oldPassword: this.oldPassword(),
      newPassword: this.newPassword()
    }).subscribe({
      next: (msg) => {
        this.submitting.set(false);
        this.successMessage.set(msg || 'Mật khẩu đã được thay đổi thành công!');
        this.resetForm();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(this.extractMessage(err));
      }
    });
  }

  /**
   * GlobalExceptionHandler của auth_service trả JSON { message }, nhưng vì request
   * khai báo responseType 'text' nên Angular đưa về err.error dạng chuỗi chưa parse.
   * Riêng 401/403 do Spring Security chặn thì body không có field message.
   */
  private extractMessage(err: unknown): string {
    const e = err as { status?: number; error?: unknown };

    if (e?.status === 401 || e?.status === 403) {
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để đổi mật khẩu.';
    }
    if (e?.status === 0) {
      return 'Không kết nối được máy chủ. Vui lòng kiểm tra lại kết nối mạng.';
    }

    const fallback = 'Đổi mật khẩu thất bại. Vui lòng thử lại.';
    const body = e?.error;

    if (typeof body === 'string') {
      try {
        return JSON.parse(body)?.message || fallback;
      } catch {
        return body.trim() || fallback;
      }
    }
    return (body as { message?: string })?.message || fallback;
  }

  private resetForm(): void {
    this.oldPassword.set('');
    this.newPassword.set('');
    this.confirmPassword.set('');
    this.showOld.set(false);
    this.showNew.set(false);
    this.showConfirm.set(false);
  }
}
