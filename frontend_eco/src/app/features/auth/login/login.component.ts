import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Mail, Lock, Eye, EyeOff, LogIn, ArrowRight } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  readonly MailIcon = Mail;
  readonly LockIcon = Lock;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly LogInIcon = LogIn;
  readonly ArrowRightIcon = ArrowRight;

  private authService = inject(AuthService);
  private router = inject(Router);

  email = signal('');
  password = signal('');
  showPassword = signal(false);
  errorMessage = signal('');

  toggleShowPassword(): void {
    this.showPassword.update(v => !v);
  }

  onSubmit(): void {
    if (!this.email() || !this.password()) {
      this.errorMessage.set('Vui lòng nhập đầy đủ Email và Mật khẩu.');
      return;
    }

    this.errorMessage.set('');

    this.authService.login({ email: this.email(), password: this.password() }).subscribe({
      next: () => {
        if (this.authService.isAdmin()) {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/profile']);
        }
      },
      error: (err) => {
        if (err.status === 0) {
          this.errorMessage.set('Không thể kết nối đến Backend API Gateway (cổng 8000). Vui lòng kiểm tra API Gateway và Auth Service đã được bật chưa.');
        } else {
          this.errorMessage.set(err?.error?.message || 'Đăng nhập không thành công. Vui lòng kiểm tra lại email & mật khẩu.');
        }
      }
    });
  }
}
