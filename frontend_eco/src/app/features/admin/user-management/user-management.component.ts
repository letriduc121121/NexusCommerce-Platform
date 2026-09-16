import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, UserPlus, Search, Edit2, Trash2, Shield, Lock, Unlock, RefreshCw, X, Check, Users } from 'lucide-angular';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { UserProfileResponse, CreateUserRequest, UpdateUserRequest } from '../../../core/models';

import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [DatePipe, FormsModule, LucideAngularModule, PaginationComponent, ModalComponent],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  readonly UserPlusIcon = UserPlus;
  readonly SearchIcon = Search;
  readonly EditIcon = Edit2;
  readonly TrashIcon = Trash2;
  readonly ShieldIcon = Shield;
  readonly LockIcon = Lock;
  readonly UnlockIcon = Unlock;
  readonly RefreshIcon = RefreshCw;
  readonly XIcon = X;
  readonly CheckIcon = Check;
  readonly UsersIcon = Users;

  private adminUserService = inject(AdminUserService);

  users = signal<UserProfileResponse[]>([]);
  searchQuery = signal('');
  isLoading = signal(false);
  successMessage = signal('');
  errorMessage = signal('');

  currentPage = signal(1);
  pageSize = 10;

  // Modal states
  isCreateModalOpen = signal(false);
  isEditModalOpen = signal(false);
  selectedUser = signal<UserProfileResponse | null>(null);

  // Form Models
  createForm = signal<CreateUserRequest>({
    email: '',
    password: '',
    fullName: '',
    phone: '',
    role: 'USER',
    status: 'ACTIVE'
  });

  editForm = signal<UpdateUserRequest>({
    fullName: '',
    phone: '',
    role: 'USER',
    status: 'ACTIVE'
  });

  filteredUsers = computed(() => {
    const list = this.users();
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return list;

    return list.filter(u =>
      u.email.toLowerCase().includes(query) ||
      (u.fullName && u.fullName.toLowerCase().includes(query)) ||
      (u.phone && u.phone.includes(query))
    );
  });

  paginatedUsers = computed(() => {
    const list = this.filteredUsers();
    const start = (this.currentPage() - 1) * this.pageSize;
    return list.slice(start, start + this.pageSize);
  });

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.currentPage.set(1);
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.adminUserService.getAllUsers().subscribe({
      next: (data) => {
        // Sắp xếp cố định (mới nhất trên đầu) -> vị trí không đổi sau khi sửa.
        this.users.set([...(data || [])].sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? '')));
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Không thể tải danh sách người dùng từ backend.');
      }
    });
  }

  openCreateModal(): void {
    this.createForm.set({
      email: '',
      password: '',
      fullName: '',
      phone: '',
      role: 'USER',
      status: 'ACTIVE'
    });
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  submitCreate(): void {
    const req = this.createForm();
    if (!req.email || !req.password || !req.role) {
      this.errorMessage.set('Vui lòng nhập đầy đủ Email, Mật khẩu và Vai trò.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminUserService.createUser(req).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.closeCreateModal();
        this.showSuccess('Tạo người dùng mới thành công!');
        this.loadUsers();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Tạo người dùng không thành công.');
      }
    });
  }

  openEditModal(user: UserProfileResponse): void {
    this.selectedUser.set(user);
    this.editForm.set({
      fullName: user.fullName || '',
      phone: user.phone || '',
      role: user.role || 'USER',
      status: user.status || 'ACTIVE'
    });
    this.isEditModalOpen.set(true);
  }

  closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.selectedUser.set(null);
  }

  submitEdit(): void {
    const user = this.selectedUser();
    if (!user) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminUserService.updateUser(user.id, this.editForm()).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.closeEditModal();
        this.showSuccess('Cập nhật thông tin người dùng thành công!');
        this.loadUsers();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Cập nhật thất bại.');
      }
    });
  }

  // Delete modal state
  deleteTarget = signal<UserProfileResponse | null>(null);
  isDeleteModalOpen = signal(false);

  openDeleteModal(user: UserProfileResponse): void {
    this.deleteTarget.set(user);
    this.isDeleteModalOpen.set(true);
  }

  confirmDeleteUser(): void {
    const user = this.deleteTarget();
    if (!user) return;

    this.isLoading.set(true);
    this.adminUserService.deleteUser(user.id).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isDeleteModalOpen.set(false);
        this.deleteTarget.set(null);
        this.showSuccess('Đã xóa người dùng thành công!');
        this.loadUsers();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Không thể xóa người dùng.');
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(''), 3000);
  }
}
