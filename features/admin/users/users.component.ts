import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';

import { ApiService } from '../../../core/services/api.service';
import { API } from '../../../core/constants/api-endpoints';

interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: string;
  active: boolean | null;
  createdAt: string | null;
  updatedAt: string | null;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.css'
})
export class UsersComponent implements OnInit {
  private readonly api = inject(ApiService);

  users: AdminUser[] = [];
  loading = false;
  errorMessage = '';
  searchTerm = '';

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';

    this.api.get<AdminUser[]>(API.USERS).subscribe({
      next: (users) => {
        this.users = Array.isArray(users) ? users : [];
        this.loading = false;
      },
      error: (error: unknown) => {
        console.error('Failed to load users:', error);

        this.errorMessage =
          'Unable to load users. Please check your access and try again.';

        this.loading = false;
      }
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm = input.value;
  }

  get filteredUsers(): AdminUser[] {
    const query = this.searchTerm.trim().toLowerCase();

    if (!query) {
      return this.users;
    }

    return this.users.filter((user) =>
      [
        user.name,
        user.email,
        user.role,
        String(user.id),
        user.active === true ? 'active' : 'inactive'
      ]
        .join(' ')
        .toLowerCase()
        .includes(query)
    );
  }

  get activeUsers(): number {
    return this.users.filter((user) => user.active === true).length;
  }

  get inactiveUsers(): number {
    return this.users.filter((user) => user.active === false).length;
  }

  get adminUsers(): number {
    return this.users.filter((user) =>
      user.role?.toUpperCase().replace(/^ROLE_/, '') === 'ADMIN'
    ).length;
  }

  trackByUserId(index: number, user: AdminUser): number {
    return user.id;
  }
}