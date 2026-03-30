import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { ToastService } from '../../../core/services/toast.service';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { SkillChipsComponent } from '../../../shared/components/skill-chips/skill-chips.component';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { UserAvatarComponent } from '../../../shared/components/user-avatar/user-avatar.component';
import { MessagingService } from '../../messaging/services/messaging.service';
import { JobService } from '../../jobs/services/job.service';
import { InvitationService } from '../../jobs/services/invitation.service';
import { Job } from '../../jobs/models/job.models';
import { Review, UserProfile } from '../models/user.models';
import { UserService } from '../services/user.service';

@Component({
  selector: 'app-public-profile',
  standalone: true,
  imports: [CommonModule, TimeAgoPipe, StarRatingComponent, SkillChipsComponent, UserAvatarComponent],
  templateUrl: './public-profile.component.html',
  styleUrl: './public-profile.component.scss',
})
export class PublicProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly messagingService = inject(MessagingService);
  private readonly keycloak = inject(KeycloakService);
  private readonly toast = inject(ToastService);
  private readonly jobService = inject(JobService);
  private readonly invitationService = inject(InvitationService);

  profile: UserProfile | null = null;
  loading = true;
  readonly startingConversation = signal(false);

  // Invite-to-job state
  readonly showInviteDropdown = signal(false);
  readonly myOpenJobs = signal<Job[]>([]);
  readonly inviting = signal(false);
  readonly isClient = computed(() =>
    this.keycloak.hasRole('ROLE_CLIENT') || this.keycloak.hasRole('CLIENT'),
  );

  reviews: Review[] = [];
  reviewsPage = 0;
  readonly reviewsPageSize = 5;
  reviewsTotalElements = 0;

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('id');
    if (userId) {
      this.loadProfile(userId);
    } else {
      this.loading = false;
    }
  }

  reviewsTotalPages(): number {
    return Math.max(1, Math.ceil(this.reviewsTotalElements / this.reviewsPageSize));
  }

  sendMessage(): void {
    if (!this.profile) return;
    const currentUserId = this.keycloak.getUserId();
    if (!currentUserId) {
      this.keycloak.login();
      return;
    }

    this.startingConversation.set(true);
    this.messagingService.getOrCreateConversation([currentUserId, this.profile.id]).subscribe({
      next: (conversation) => {
        this.startingConversation.set(false);
        this.router.navigate(['/messages', conversation.id]);
      },
      error: () => this.startingConversation.set(false),
    });
  }

  previousReviewsPage(): void {
    if (this.reviewsPage === 0 || !this.profile) return;
    this.reviewsPage--;
    this.loadReviews(this.profile.id);
  }

  nextReviewsPage(): void {
    if (this.reviewsPage + 1 >= this.reviewsTotalPages() || !this.profile) return;
    this.reviewsPage++;
    this.loadReviews(this.profile.id);
  }

  private loadProfile(userId: string): void {
    this.loading = true;
    this.userService.getProfile(userId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
        this.loadReviews(userId);
      },
      error: () => (this.loading = false),
    });
  }

  private loadReviews(userId: string): void {
    this.userService.getReviews(userId, this.reviewsPage, this.reviewsPageSize).subscribe({
      next: (response) => {
        this.reviews = response.content;
        this.reviewsTotalElements = response.totalElements;
      },
    });
  }

  toggleInviteDropdown(): void {
    if (!this.showInviteDropdown()) {
      // Load client's OPEN jobs lazily
      if (this.myOpenJobs().length === 0) {
        this.jobService.getMyJobs(0, 50).subscribe({
          next: (res) => this.myOpenJobs.set(res.content.filter((j) => j.status === 'OPEN')),
        });
      }
    }
    this.showInviteDropdown.set(!this.showInviteDropdown());
  }

  inviteToJob(job: Job): void {
    if (!this.profile) return;
    this.inviting.set(true);
    this.invitationService.invite(job.id, this.profile.id).subscribe({
      next: () => {
        this.inviting.set(false);
        this.showInviteDropdown.set(false);
        this.toast.success(`Invitation sent for "${job.title}"`);
      },
      error: (err) => {
        this.inviting.set(false);
        const message = err?.error?.message ?? 'Failed to send invitation.';
        this.toast.error(message);
      },
    });
  }
}
