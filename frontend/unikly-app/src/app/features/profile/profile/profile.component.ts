import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe, LowerCasePipe } from '@angular/common';
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
    DecimalPipe,
    LowerCasePipe,
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

  readonly profile = signal<UserProfile | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly editMode = signal(false);
  readonly saving = signal(false);
  readonly activeTab = signal<'profile' | 'reviews'>('profile');
  readonly skillSuggestions = signal<string[]>([]);
  readonly reviews = signal<Review[]>([]);

  editSkills: string[] = [];
  reviewsPage = 0;
  reviewsTotalElements = 0;
  readonly reviewsPageSize = 5;
  readonly currencies = ['USD', 'EUR', 'XAF', 'GBP'];
  readonly skillInputControl = new FormControl('');

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
          this.jobService
            .getSuggestions(value.trim())
            .subscribe((suggestions) => this.skillSuggestions.set(suggestions));
        } else {
          this.skillSuggestions.set([]);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleEditMode(): void {
    this.editMode.set(!this.editMode());
    const p = this.profile();
    if (!this.editMode() || !p) return;

    this.form.patchValue({
      displayName: p.displayName,
      bio: p.bio,
      hourlyRate: p.hourlyRate,
      currency: p.currency || 'USD',
      location: p.location,
    });
    this.editSkills = [...p.skills];

    this.portfolioLinks.clear();
    for (const link of p.portfolioLinks) {
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

    this.saving.set(true);
    const payload = {
      ...this.form.value,
      skills: this.editSkills,
      portfolioLinks: this.portfolioLinks.value.filter((link: string) => link?.trim()),
    };

    this.userService.updateMyProfile(payload).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.editMode.set(false);
        this.saving.set(false);
        this.toast.success('Profile updated successfully.');
      },
      error: () => this.saving.set(false),
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
    this.loading.set(true);
    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loading.set(false);
        this.loadReviews();
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
  }

  private loadReviews(): void {
    const p = this.profile();
    if (!p) return;
    this.userService.getReviews(p.id, this.reviewsPage, this.reviewsPageSize).subscribe({
      next: (response) => {
        this.reviews.set(response.content);
        this.reviewsTotalElements = response.totalElements;
      },
    });
  }
}
