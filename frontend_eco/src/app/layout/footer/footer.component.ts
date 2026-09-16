import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  MapPin,
  Phone,
  Clock,
  Mail,
  ExternalLink,
  ShieldCheck,
  Truck,
  RotateCcw,
  CreditCard
} from 'lucide-angular';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss'
})
export class FooterComponent {
  readonly MapPinIcon = MapPin;
  readonly PhoneIcon = Phone;
  readonly ClockIcon = Clock;
  readonly MailIcon = Mail;
  readonly ExternalLinkIcon = ExternalLink;

  readonly currentYear = new Date().getFullYear();
  readonly storeAddress = 'Số 36 Hàng Bài, Hoàn Kiếm, Hà Nội';

  readonly features = [
    { icon: ShieldCheck, title: 'Bảo hành chính hãng 100%' },
    { icon: Truck, title: 'Giao hàng hỏa tốc toàn quốc' },
    { icon: RotateCcw, title: '1 đổi 1 trong 30 ngày' },
    { icon: CreditCard, title: 'Trả góp 0% lãi suất' }
  ];

  readonly supportLinks = [
    { label: 'Chính sách bảo hành', link: '/about' },
    { label: 'Hướng dẫn mua trả góp', link: '/about' },
    { label: 'Chính sách giao hàng', link: '/about' },
    { label: 'Tra cứu đơn hàng', link: '/about' },
    { label: 'Trung tâm hỗ trợ', link: '/about' }
  ];

  readonly socials = [
    { name: 'FB', url: '#' },
    { name: 'IG', url: '#' },
    { name: 'YT', url: '#' },
    { name: 'TT', url: '#' }
  ];

  openGoogleMaps(): void {
    const query = encodeURIComponent('36 Hàng Bài, Hoàn Kiếm, Hà Nội');
    window.open(`https://www.google.com/maps/search/?api=1&query=${query}`, '_blank');
  }
}

