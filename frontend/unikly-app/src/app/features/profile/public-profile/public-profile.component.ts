import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { KeycloakService } from '../../../core/auth/keycloak.service';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { SkillChipsComponent } from '../../../shared/components/skill-chips/skill-chips.component';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { UserAvatarComponent } from '../../../shared/components/user-avatar/user-avatar.component';
import { MessagingService } from '../../messaging/services/messaging.service';
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

  profile: UserProfile | null = null;
  loading = true;
  readonly startingConversation = signal(false);

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
}
