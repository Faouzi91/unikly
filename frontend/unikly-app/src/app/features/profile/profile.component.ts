import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { StarRatingComponent } from '../../shared/components/star-rating/star-rating.component';
import { SkillChipsComponent } from '../../shared/components/skill-chips/skill-chips.component';
import { UserAvatarComponent } from '../../shared/components/user-avatar/user-avatar.component';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';
import { UserService } from './services/user.service';
import { JobService } from '../jobs/services/job.service';
import { KeycloakService } from '../../core/auth/keycloak.service';
import { UserProfile, Review } from './models/user.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatDividerModule,
    MatSnackBarModule,
    MatPaginatorModule,
    StarRatingComponent,
    SkillChipsComponent,
    UserAvatarComponent,
    TimeAgoPipe,
  ],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit, OnDestroy {
  private readonly userService = inject(UserService);
  private readonly jobService = inject(JobService);
  private readonly keycloak = inject(KeycloakService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroy$ = new Subject<void>();

  profile: UserProfile | null = null;
  loading = true;
  editMode = false;
  saving = false;
  currencies = ['USD', 'EUR', 'XAF', 'GBP'];

  // Skills editing
  editSkills: string[] = [];
  skillInputControl = new FormControl('');
  skillSuggestions: string[] = [];

  // Reviews
  reviews: Review[] = [];
  reviewsPage = 0;
  reviewsPageSize = 5;
  reviewsTotalElements = 0;

  form: FormGroup = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    bio: [''],
    hourlyRate: [null],
    currency: ['USD'],
    location: [''],
    portfolioLinks: this.fb.array([]),
  });

  get portfolioLinks(): FormArray {
    return this.form.get('portfolioLinks') as FormArray;
  }

  getPortfolioControl(index: number): FormControl {
    return this.portfolioLinks.at(index) as FormControl;
  }

  ngOnInit(): void {
    this.loadProfile();

    this.skillInputControl.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((value) => {
        if (value && value.length >= 2) {
          this.jobService
            .getSuggestions(value)
            .subscribe((suggestions) => (this.skillSuggestions = suggestions));
        } else {
          this.skillSuggestions = [];
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadProfile(): void {
    this.loading = true;
    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
        this.loadReviews();
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private loadReviews(): void {
    if (!this.profile) return;
    this.userService
      .getReviews(this.profile.id, this.reviewsPage, this.reviewsPageSize)
      .subscribe({
        next: (response) => {
          this.reviews = response.content;
          this.reviewsTotalElements = response.totalElements;
        },
      });
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;
    if (this.editMode && this.profile) {
      this.form.patchValue({
        displayName: this.profile.displayName,
        bio: this.profile.bio,
        hourlyRate: this.profile.hourlyRate,
        currency: this.profile.currency || 'USD',
        location: this.profile.location,
      });
      this.editSkills = [...this.profile.skills];

      this.portfolioLinks.clear();
      for (const link of this.profile.portfolioLinks) {
        this.portfolioLinks.push(this.fb.control(link));
      }
    }
  }

  addSkill(event: MatAutocompleteSelectedEvent): void {
    const skill = event.option.value;
    if (!this.editSkills.includes(skill)) {
      this.editSkills.push(skill);
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.editSkills = this.editSkills.filter((s) => s !== skill);
  }

  addPortfolioLink(): void {
    this.portfolioLinks.push(this.fb.control(''));
  }

  removePortfolioLink(index: number): void {
    this.portfolioLinks.removeAt(index);
  }

  onSave(): void {
    if (this.form.invalid) return;
    this.saving = true;

    const data = {
      ...this.form.value,
      skills: this.editSkills,
      portfolioLinks: this.portfolioLinks.value.filter(
        (link: string) => link?.trim(),
      ),
    };

    this.userService.updateMyProfile(data).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.editMode = false;
        this.saving = false;
        this.snackBar.open('Profile updated!', 'Close', { duration: 3000 });
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  onReviewsPageChange(event: PageEvent): void {
    this.reviewsPage = event.pageIndex;
    this.reviewsPageSize = event.pageSize;
    this.loadReviews();
  }
}
