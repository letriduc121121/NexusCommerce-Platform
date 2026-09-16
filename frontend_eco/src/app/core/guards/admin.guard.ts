import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  if (authService.currentUser()) {
    if (authService.isAdmin()) return true;
    router.navigate(['/login']);
    return false;
  }

  return authService.fetchProfile().pipe(
    map(user => {
      if (user?.role === 'ADMIN') return true;
      router.navigate(['/login']);
      return false;
    })
  );
};

