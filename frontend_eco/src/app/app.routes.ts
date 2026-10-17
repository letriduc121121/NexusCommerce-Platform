import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent),
        title: 'AULA SHOP | Trang Chủ Thời Trang Đương Đại'
      },
      {
        path: 'products/:id',
        loadComponent: () => import('./features/product-detail/product-detail.component').then(m => m.ProductDetailComponent),
        title: 'AULA SHOP | Chi Tiết Sản Phẩm'
      },
      {
        path: 'about',
        loadComponent: () => import('./features/about/about.component').then(m => m.AboutComponent),
        title: 'AULA SHOP | Giới Thiệu & Lịch Sử Thành Lập'
      },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
        title: 'AULA SHOP | Đăng Nhập'
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
        title: 'AULA SHOP | Đăng Ký'
      },
      {
        path: 'checkout',
        canActivate: [authGuard],
        loadComponent: () => import('./features/checkout/checkout.component').then(m => m.CheckoutComponent),
        title: 'AULA SHOP | Chi Tiết Đặt Hàng & Thanh Toán'
      },
      {
        path: 'checkout/success',
        loadComponent: () => import('./features/checkout/order-success.component').then(m => m.OrderSuccessComponent),
        title: 'AULA SHOP | Thanh Toán Thành Công'
      },
      {
        path: 'checkout/vnpay-callback',
        loadComponent: () => import('./features/checkout/order-success.component').then(m => m.OrderSuccessComponent),
        title: 'AULA SHOP | Thanh Toán Thành Công'
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent),
        children: [
          {
            path: '',
            loadComponent: () => import('./features/profile/profile-info/profile-info.component').then(m => m.ProfileInfoComponent),
            title: 'AULA SHOP | Thông Tin Cá Nhân'
          },
          {
            path: 'orders',
            loadComponent: () => import('./features/profile/order-history/order-history.component').then(m => m.OrderHistoryComponent),
            title: 'AULA SHOP | Lịch Sử Đơn Hàng'
          },
          {
            path: 'orders/review/:orderItemId',
            loadComponent: () => import('./features/profile/write-review/write-review.component').then(m => m.WriteReviewComponent),
            title: 'AULA SHOP | Viết Đánh Giá Sản Phẩm'
          },
          {
            path: 'change-password',
            loadComponent: () => import('./features/profile/change-password/change-password.component').then(m => m.ChangePasswordComponent),
            title: 'AULA SHOP | Đổi Mật Khẩu'
          }
        ]
      },
      {
        path: 'review/:orderItemId',
        canActivate: [authGuard],
        loadComponent: () => import('./features/profile/write-review/write-review.component').then(m => m.WriteReviewComponent),
        title: 'AULA SHOP | Viết Đánh Giá Sản Phẩm'
      }
    ]
  },

  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin-layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard-overview/dashboard-overview.component').then(m => m.DashboardOverviewComponent),
        title: 'AULA SHOP | Dashboard Tổng Quan (Admin)'
      },
      {
        path: 'users',
        loadComponent: () => import('./features/admin/user-management/user-management.component').then(m => m.UserManagementComponent),
        title: 'AULA SHOP | Quản Lý Người Dùng (Admin)'
      },
      {
        path: 'products',
        loadComponent: () => import('./features/admin/product-management/product-management.component').then(m => m.ProductManagementComponent),
        title: 'AULA SHOP | Quản Lý Sản Phẩm (Admin)'
      },
      {
        path: 'inventory',
        loadComponent: () => import('./features/admin/inventory-management/inventory-management.component').then(m => m.InventoryManagementComponent),
        title: 'AULA SHOP | Quản Lý Tồn Kho (Admin)'
      },
      {
        path: 'orders',
        loadComponent: () => import('./features/admin/order-management/order-management.component').then(m => m.OrderManagementComponent),
        title: 'AULA SHOP | Quản Lý Đơn Hàng (Admin)'
      },
      {
        path: 'vouchers',
        loadComponent: () => import('./features/admin/voucher-management/voucher-management.component').then(m => m.VoucherManagementComponent),
        title: 'AULA SHOP | Quản Lý Voucher (Admin)'
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
