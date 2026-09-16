import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, User, Mail, Phone, Lock, Eye, EyeOff, UserPlus, ArrowRight } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './register.component.html',
  styleUrl: '../login/login.component.scss'
})
export class RegisterComponent {
  readonly UserIcon = User;
  readonly MailIcon = Mail;
  readonly PhoneIcon = Phone;
  readonly LockIcon = Lock;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly UserPlusIcon = UserPlus;
  readonly ArrowRightIcon = ArrowRight;

  private authService = inject(AuthService);
  private router = inject(Router);

  fullName = signal('');
  email = signal('');
  phone = signal('');
  password = signal('');
  confirmPassword = signal('');
  showPassword = signal(false);
  errorMessage = signal('');

  toggleShowPassword(): void {
    this.showPassword.update(v => !v);
  }

  onSubmit(): void {
    if (!this.fullName() || !this.email() || !this.phone() || !this.password()) {
      this.errorMessage.set('Vui lòng điền đầy đủ các thông tin bắt buộc.');
      return;
    }

    if (this.password() !== this.confirmPassword()) {
      this.errorMessage.set('Mật khẩu nhập lại không khớp.');
      return;
    }

    this.errorMessage.set('');

    this.authService.register({
      fullName: this.fullName(),
      phone: this.phone(),
      email: this.email(),
      password: this.password()
    }).subscribe({
      next: () => {
        this.router.navigate(['/profile']);
      },
      error: (err) => {
        if (err.status === 0) {
          this.errorMessage.set('Không thể kết nối đến Backend API Gateway (cổng 8000). Vui lòng kiểm tra API Gateway và Auth Service đã được bật chưa.');
        } else {
          this.errorMessage.set(err?.error?.message || 'Đăng ký không thành công. Email hoặc tài khoản có thể đã tồn tại.');
        }
      }
    });
  }
}
