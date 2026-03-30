import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  Job,
  CreateJobRequest,
  UpdateJobRequest,
  SubmitProposalRequest,
  StatusTransitionRequest,
  Proposal,
  PageResponse,
  JobSearchResult,
  FreelancerSearchResult,
  MatchEntry,
  EditDecision,
} from '../models/job.models';

@Injectable({
  providedIn: 'root',
})
export class JobService {
  constructor(private readonly api: ApiService) {}

  getJobs(params: {
    q?: string;
    skills?: string;
    minBudget?: number;
    maxBudget?: number;
    page: number;
    size: number;
  }): Observable<PageResponse<JobSearchResult>> {
    const queryParams: Record<string, string | number> = {
      page: params.page,
      size: params.size,
    };
    if (params.q) queryParams['q'] = params.q;
    if (params.skills) queryParams['skills'] = params.skills;
    if (params.minBudget != null) queryParams['minBudget'] = params.minBudget;
    if (params.maxBudget != null) queryParams['maxBudget'] = params.maxBudget;
    return this.api.get<PageResponse<JobSearchResult>>(
      '/v1/search/jobs',
      queryParams,
    );
  }

  getMyJobs(
    page: number,
    size: number,
  ): Observable<PageResponse<Job>> {
    return this.api.get<PageResponse<Job>>('/v1/jobs', {
      page,
      size,
    });
  }

  getMyContracts(
    page: number,
    size: number,
  ): Observable<PageResponse<Job>> {
    return this.api.get<PageResponse<Job>>('/v1/jobs/my-contracts', {
      page,
      size,
    });
  }

  getJob(id: string): Observable<Job> {
    return this.api.get<Job>(`/v1/jobs/${id}`);
  }

  createJob(data: CreateJobRequest): Observable<Job> {
    return this.api.post<Job>('/v1/jobs', data);
  }

  updateJob(id: string, data: UpdateJobRequest, confirmed = false): Observable<Job> {
    return this.api.patch<Job>(`/v1/jobs/${id}`, data, { confirmed });
  }

  checkEditEligibility(id: string, request: UpdateJobRequest): Observable<EditDecision> {
    return this.api.post<EditDecision>(`/v1/jobs/${id}/check-edit`, request);
  }

  cancelJob(id: string): Observable<void> {
    return this.api.post<void>(`/v1/jobs/${id}/cancel`, null);
  }

  updateJobStatus(id: string, status: string): Observable<Job> {
    return this.api.patch<Job>(`/v1/jobs/${id}/status`, {
      status,
    } as StatusTransitionRequest);
  }

  submitProposal(
    jobId: string,
    data: SubmitProposalRequest,
  ): Observable<Proposal> {
    return this.api.post<Proposal>(`/v1/jobs/${jobId}/proposals`, data);
  }

  getProposals(jobId: string): Observable<PageResponse<Proposal>> {
    return this.api.get<PageResponse<Proposal>>(`/v1/jobs/${jobId}/proposals`);
  }

  acceptProposal(jobId: string, proposalId: string): Observable<Proposal> {
    return this.api.patch<Proposal>(
      `/v1/jobs/${jobId}/proposals/${proposalId}/accept`,
    );
  }

  rejectProposal(jobId: string, proposalId: string): Observable<Proposal> {
    return this.api.patch<Proposal>(
      `/v1/jobs/${jobId}/proposals/${proposalId}/reject`,
    );
  }

  resubmitProposal(jobId: string, proposalId: string, request: SubmitProposalRequest): Observable<Proposal> {
    return this.api.put<Proposal>(
      `/v1/jobs/${jobId}/proposals/${proposalId}/resubmit`,
      request,
    );
  }

  getMyProposal(jobId: string): Observable<Proposal> {
    return this.api.get<Proposal>(`/v1/jobs/${jobId}/proposals/mine`);
  }

  submitDelivery(jobId: string, note = ''): Observable<Job> {
    return this.api.patch<Job>(`/v1/jobs/${jobId}/submit-delivery`, { note });
  }

  getMatches(jobId: string): Observable<MatchEntry[]> {
    return this.api.get<MatchEntry[]>('/v1/matches', { jobId });
  }

  searchFreelancers(params: {
    q?: string;
    skills?: string;
    minRating?: number;
    page: number;
    size: number;
  }): Observable<PageResponse<FreelancerSearchResult>> {
    const queryParams: Record<string, string | number> = {
      page: params.page,
      size: params.size,
    };
    if (params.q) queryParams['q'] = params.q;
    if (params.skills) queryParams['skills'] = params.skills;
    if (params.minRating != null) queryParams['minRating'] = params.minRating;
    return this.api.get<PageResponse<FreelancerSearchResult>>(
      '/v1/search/freelancers',
      queryParams,
    );
  }

  getSuggestions(query: string): Observable<string[]> {
    return this.api.get<string[]>('/v1/search/suggestions', {
      q: query,
      type: 'skill',
    });
  }
}
