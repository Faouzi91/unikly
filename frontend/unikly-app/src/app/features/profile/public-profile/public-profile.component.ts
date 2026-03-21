import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { SkillChipsComponent } from '../../../shared/components/skill-chips/skill-chips.component';
import { UserAvatarComponent } from '../../../shared/components/user-avatar/user-avatar.component';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { UserService } from '../services/user.service';
import { UserProfile, Review } from '../models/user.models';
import { MessagingService } from '../../messaging/services/messaging.service';
import { KeycloakService } from '../../../core/auth/keycloak.service';

@Component({
  selector: 'app-public-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    StarRatingComponent,
    SkillChipsComponent,
    UserAvatarComponent,
    TimeAgoPipe,
  ],
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
  startingConversation = signal(false);

  reviews: Review[] = [];
  reviewsPage = 0;
  reviewsPageSize = 5;
  reviewsTotalElements = 0;

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('id')!;
    this.loadProfile(userId);
  }

  sendMessage(): void {
    if (!this.profile) return;
    const currentUserId = this.keycloak.getUserId();
    if (!currentUserId) {
      this.keycloak.login();
      return;
    }

    this.startingConversation.set(true);
    this.messagingService
      .getOrCreateConversation([currentUserId, this.profile.id])
      .subscribe({
        next: (conversation) => {
          this.startingConversation.set(false);
          this.router.navigate(['/messages', conversation.id]);
        },
        error: () => this.startingConversation.set(false),
      });
  }

  private loadProfile(userId: string): void {
    this.loading = true;
    this.userService.getProfile(userId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
        this.loadReviews(userId);
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private loadReviews(userId: string): void {
    this.userService
      .getReviews(userId, this.reviewsPage, this.reviewsPageSize)
      .subscribe({
        next: (response) => {
          this.reviews = response.content;
          this.reviewsTotalElements = response.totalElements;
        },
      });
  }

  onReviewsPageChange(event: PageEvent): void {
    this.reviewsPage = event.pageIndex;
    this.reviewsPageSize = event.pageSize;
    if (this.profile) {
      this.loadReviews(this.profile.id);
    }
  }
}
