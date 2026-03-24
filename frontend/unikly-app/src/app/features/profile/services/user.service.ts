import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  UserProfile,
  UserProfileRequest,
  Review,
  PageResponse,
} from '../models/user.models';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  constructor(private readonly api: ApiService) {}

  getMyProfile(): Observable<UserProfile> {
    return this.api.get<UserProfile>('/users/me');
  }

  updateMyProfile(data: UserProfileRequest): Observable<UserProfile> {
    return this.api.put<UserProfile>('/users/me', data);
  }

  /**
   * Upload profile avatar image. Sends multipart/form-data to backend.
   * Returns { avatarUrl: string } on success.
   */
  uploadAvatar(file: File): Observable<{ avatarUrl: string }> {
    const form = new FormData();
    form.append('file', file);
    return this.api.postFormData<{ avatarUrl: string }>('/users/me/avatar', form);
  }

  getProfile(id: string): Observable<UserProfile> {
    return this.api.get<UserProfile>(`/users/${id}`);
  }

  getReviews(
    userId: string,
    page: number,
    size: number,
  ): Observable<PageResponse<Review>> {
    return this.api.get<PageResponse<Review>>(`/users/${userId}/reviews`, {
      page,
      size,
    });
  }

  createReview(
    userId: string,
    jobId: string,
    rating: number,
    comment: string,
  ): Observable<Review> {
    return this.api.post<Review>(`/users/${userId}/reviews`, { jobId, rating, comment });
  }
}
