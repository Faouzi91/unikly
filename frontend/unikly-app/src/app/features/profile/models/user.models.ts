export interface UserProfile {
  id: string;
  displayName: string;
  bio: string;
  avatarUrl: string | null;
  role: 'CLIENT' | 'FREELANCER';
  skills: string[];
  hourlyRate: number | null;
  currency: string;
  location: string;
  portfolioLinks: string[];
  averageRating: number;
  totalReviews: number;
  createdAt: string;
  updatedAt: string;
}

export interface UserProfileRequest {
  displayName: string;
  bio: string;
  avatarUrl?: string | null;
  role?: 'CLIENT' | 'FREELANCER';
  skills: string[];
  hourlyRate: number | null;
  currency: string;
  location: string;
  portfolioLinks: string[];
}

export interface Review {
  id: string;
  reviewerId: string;
  revieweeId: string;
  jobId: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
