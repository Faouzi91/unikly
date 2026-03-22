import { Component, OnInit, inject } from '@angular/core';
import { NgClass, DecimalPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { ThemeService } from '../../../core/services/theme.service';
import { AdminService, AdminStats, UserProfileResponse } from '../../../core/services/admin.service';

interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  latency: number;
  lastSync: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [NgClass, DecimalPipe, CurrencyPipe, DatePipe],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  themeService = inject(ThemeService);
  adminService = inject(AdminService);
  stats: AdminStats | null = null;
  users: UserProfileResponse[] = [];
  totalUsersInDirectory: number = 0;

  ngOnInit(): void {
    this.adminService.getDashboardStats().subscribe({
      next: (res) => this.stats = res,
      error: (err) => console.error('Failed to load admin stats', err)
    });

    this.adminService.getUserDirectory(0, 50).subscribe({
      next: (res) => {
        this.users = res.content;
        this.totalUsersInDirectory = res.totalElements;
      },
      error: (err) => console.error('Failed to load user directory', err)
    });
  }
}
