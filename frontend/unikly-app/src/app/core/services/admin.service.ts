import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AdminStats {
  totalUsers: number;
  totalActiveJobs: number;
  totalEscrowVolume: number;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserProfileResponse {
  id: string;
  displayName: string;
  role: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private userApiUrl = environment.apiUrl + '/users/admin/stats';
  private jobApiUrl = environment.apiUrl + '/jobs/admin/stats';
  private paymentApiUrl = environment.apiUrl + '/api/v1/payments/admin/stats';

  getDashboardStats(): Observable<AdminStats> {
    return forkJoin({
      users: this.http.get<{ totalUsers: number }>(this.userApiUrl),
      jobs: this.http.get<{ totalActiveJobs: number }>(this.jobApiUrl),
      payments: this.http.get<{ totalEscrowVolume: number }>(this.paymentApiUrl)
    }).pipe(
      map((res: any) => ({
        totalUsers: res.users.totalUsers,
        totalActiveJobs: res.jobs.totalActiveJobs,
        totalEscrowVolume: res.payments.totalEscrowVolume
      }))
    );
  }

  getUserDirectory(page: number = 0, size: number = 20): Observable<PageResponse<UserProfileResponse>> {
    return this.http.get<PageResponse<UserProfileResponse>>(`${environment.apiUrl}/users/admin/directory`, {
      params: { page, size }
    });
  }
}
