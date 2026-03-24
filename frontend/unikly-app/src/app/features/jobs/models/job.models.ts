export interface Job {
  id: string;
  clientId: string;
  title: string;
  description: string;
  budget: number;
  currency: string;
  skills: string[];
  status: JobStatus;
  proposalCount?: number;
  createdAt: string;
  updatedAt: string;
}

export type JobStatus =
  | 'DRAFT'
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'CLOSED'
  | 'CANCELLED'
  | 'DISPUTED'
  | 'REFUNDED';

export interface Proposal {
  id: string;
  jobId: string;
  freelancerId: string;
  freelancerName?: string;
  proposedBudget: number;
  coverLetter: string;
  status: ProposalStatus;
  createdAt: string;
}

export type ProposalStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export interface Contract {
  id: string;
  jobId: string;
  clientId: string;
  freelancerId: string;
  agreedBudget: number;
  terms: string;
  status: ContractStatus;
  startedAt: string;
  completedAt?: string;
}

export type ContractStatus = 'ACTIVE' | 'COMPLETED' | 'TERMINATED';

export interface CreateJobRequest {
  title: string;
  description: string;
  budget: number;
  currency: string;
  skills: string[];
}

export interface UpdateJobRequest {
  title?: string;
  description?: string;
  budget?: number;
  currency?: string;
  skills?: string[];
}

export interface SubmitProposalRequest {
  proposedBudget: number;
  coverLetter: string;
}

export interface StatusTransitionRequest {
  status: JobStatus;
}

export interface JobSearchResult {
  jobId: string;
  title: string;
  description: string;
  skills: string[];
  budget: number;
  currency: string;
  status: string;
  clientId: string;
  createdAt: string;
  score: number;
}

export interface FreelancerSearchResult {
  userId: string;
  displayName: string;
  skills: string[];
  hourlyRate: number;
  averageRating: number;
  location: string;
  bio: string;
  score: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MatchEntry {
  freelancerId: string;
  freelancerName?: string;
  skills: string[];
  matchScore: number;
  hourlyRate?: number;
  averageRating?: number;
}
