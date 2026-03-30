import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

export interface InviteFreelancerRequest {
  freelancerId: string;
  message?: string;
}

export interface InvitationResponse {
  id: string;
  jobId: string;
  clientId: string;
  freelancerId: string;
  status: string;
  message?: string;
  createdAt: string;
}

export interface JobSummary {
  id: string;
  title: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class InvitationService {
  private readonly api = inject(ApiService);

  invite(jobId: string, freelancerId: string, message?: string): Observable<InvitationResponse> {
    return this.api.post<InvitationResponse>(`/v1/jobs/${jobId}/invitations`, {
      freelancerId,
      message,
    });
  }

  getJobInvitations(jobId: string, page = 0, size = 20): Observable<{ content: InvitationResponse[] }> {
    return this.api.get(`/v1/jobs/${jobId}/invitations`, { page, size });
  }

  getMyInvitations(page = 0, size = 20): Observable<{ content: InvitationResponse[] }> {
    return this.api.get('/v1/jobs/invitations/mine', { page, size });
  }
}
