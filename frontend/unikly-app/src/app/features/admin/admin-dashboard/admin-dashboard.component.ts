import { Component, OnInit, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { ThemeService } from '../../../core/services/theme.service';

interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  latency: number;
  lastSync: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [NgClass],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  themeService = inject(ThemeService);

  services: ServiceHealth[] = [
    { name: 'API Gateway', status: 'UP', latency: 4, lastSync: 'Now' },
    { name: 'Auth Server', status: 'UP', latency: 8, lastSync: 'Now' },
    { name: 'User Service', status: 'UP', latency: 12, lastSync: 'Now' },
    { name: 'Job Service', status: 'UP', latency: 15, lastSync: 'Now' },
    { name: 'Payment Flow', status: 'UP', latency: 22, lastSync: 'Now' },
    { name: 'Matching Engine', status: 'DEGRADED', latency: 450, lastSync: 'Now' },
    { name: 'Real-time Messaging', status: 'UP', latency: 8, lastSync: 'Now' },
    { name: 'Search Index', status: 'UP', latency: 18, lastSync: 'Now' }
  ];

  ngOnInit(): void {}
}
