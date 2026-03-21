import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
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
import { ToastService } from '../../../core/services/toast.service';
import { JobService } from '../../jobs/services/job.service';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';
import { SkillChipsComponent } from '../../../shared/components/skill-chips/skill-chips.component';
import { StarRatingComponent } from '../../../shared/components/star-rating/star-rating.component';
import { UserAvatarComponent } from '../../../shared/components/user-avatar/user-avatar.component';
import { Review, UserProfile } from '../models/user.models';
import { UserService } from '../services/user.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    StarRatingComponent,
    SkillChipsComponent,
    UserAvatarComponent,
    TimeAgoPipe,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit, OnDestroy {
  private readonly userService = inject(UserService);
  private readonly jobService = inject(JobService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly destroy$ = new Subject<void>();

  profile: UserProfile | null = null;
  loading = true;
  editMode = false;
  saving = false;
  currencies = ['USD', 'EUR', 'XAF', 'GBP'];
  activeTab: 'profile' | 'reviews' = 'profile';

  editSkills: string[] = [];
  readonly skillInputControl = new FormControl('');
  skillSuggestions: string[] = [];

  reviews: Review[] = [];
  reviewsPage = 0;
  readonly reviewsPageSize = 5;
  reviewsTotalElements = 0;

  readonly form: FormGroup = this.fb.group({
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

  reviewsTotalPages(): number {
    return Math.max(1, Math.ceil(this.reviewsTotalElements / this.reviewsPageSize));
  }

  ngOnInit(): void {
    this.loadProfile();

    this.skillInputControl.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((value) => {
        if (value && value.trim().length >= 2) {
          this.jobService.getSuggestions(value.trim()).subscribe((suggestions) => (this.skillSuggestions = suggestions));
        } else {
          this.skillSuggestions = [];
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;
    if (!this.editMode || !this.profile) return;

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

  addSkill(skillInput?: string): void {
    const skill = (skillInput ?? this.skillInputControl.value ?? '').trim();
    if (!skill) return;
    if (!this.editSkills.includes(skill)) {
      this.editSkills.push(skill);
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.editSkills = this.editSkills.filter((item) => item !== skill);
  }

  addPortfolioLink(): void {
    this.portfolioLinks.push(this.fb.control(''));
  }

  removePortfolioLink(index: number): void {
    this.portfolioLinks.removeAt(index);
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    const payload = {
      ...this.form.value,
      skills: this.editSkills,
      portfolioLinks: this.portfolioLinks.value.filter((link: string) => link?.trim()),
    };

    this.userService.updateMyProfile(payload).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.editMode = false;
        this.saving = false;
        this.toast.success('Profile updated successfully.');
      },
      error: () => (this.saving = false),
    });
  }

  previousReviewsPage(): void {
    if (this.reviewsPage === 0) return;
    this.reviewsPage--;
    this.loadReviews();
  }

  nextReviewsPage(): void {
    if (this.reviewsPage + 1 >= this.reviewsTotalPages()) return;
    this.reviewsPage++;
    this.loadReviews();
  }

  private loadProfile(): void {
    this.loading = true;
    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
        this.loadReviews();
      },
      error: () => (this.loading = false),
    });
  }

  private loadReviews(): void {
    if (!this.profile) return;
    this.userService.getReviews(this.profile.id, this.reviewsPage, this.reviewsPageSize).subscribe({
      next: (response) => {
        this.reviews = response.content;
        this.reviewsTotalElements = response.totalElements;
      },
    });
  }
}
