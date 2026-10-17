import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideAngularModule, User, Package, LogOut, ShieldCheck, KeyRound, Coins } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, LucideAngularModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  readonly UserIcon = User;
  readonly PackageIcon = Package;
  readonly LogOutIcon = LogOut;
  readonly ShieldCheckIcon = ShieldCheck;
  readonly KeyRoundIcon = KeyRound;
  readonly CoinsIcon = Coins;

  authService = inject(AuthService);

  ngOnInit(): void {
    this.authService.fetchProfile().subscribe();
  }

  logout(): void {
    this.authService.logout();
  }
}
