import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, User as UserIcon, Mail, Phone, MapPin, Save, CheckCircle } from 'lucide-angular';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-profile-info',
  standalone: true,
  imports: [FormsModule, LucideAngularModule],
  templateUrl: './profile-info.component.html',
  styleUrl: './profile-info.component.scss'
})
export class ProfileInfoComponent implements OnInit {
  readonly UserIcon = UserIcon;
  readonly MailIcon = Mail;
  readonly PhoneIcon = Phone;
  readonly MapPinIcon = MapPin;
  readonly SaveIcon = Save;
  readonly CheckCircleIcon = CheckCircle;

  private authService = inject(AuthService);

  fullName = signal('');
  email = signal('');
  phone = signal('');
  address = signal('');

  successMessage = signal('');

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.fullName.set(user.fullName || '');
      this.email.set(user.email || '');
      this.phone.set(user.phone || '');
      this.address.set(user.address || '');
    } else {
      this.authService.fetchProfile().subscribe(u => {
        if (u) {
          this.fullName.set(u.fullName || '');
          this.email.set(u.email || '');
          this.phone.set(u.phone || '');
          this.address.set(u.address || '');
        }
      });
    }
  }

  onSubmit(): void {
    this.authService.updateProfile({
      fullName: this.fullName(),
      phone: this.phone()
    }).subscribe({
      next: () => {
        this.successMessage.set('Đã cập nhật thông tin cá nhân thành công!');
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {}
    });
  }
}
