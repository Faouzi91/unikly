import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StarRatingComponent } from '../../../../shared/components/star-rating/star-rating.component';
import { ReviewRequest } from '../../../profile/models/user.models';

export interface ReviewDialogData {
  jobId: string;
  jobTitle: string;
  revieweeId: string;
  revieweeName: string;
}

@Component({
  selector: 'app-review-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, StarRatingComponent],
  templateUrl: './review-dialog.html',
  styleUrl: './review-dialog.scss',
})
export class ReviewDialog {
  private readonly fb = inject(FormBuilder);

  @Input({ required: true }) data!: ReviewDialogData;
  @Input() submitting = false;
  @Output() cancel = new EventEmitter<void>();
  @Output() submitReview = new EventEmitter<ReviewRequest>();

  readonly form: FormGroup = this.fb.group({
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
  });

  onRatingChange(star: number): void {
    this.form.patchValue({ rating: star });
    this.form.get('rating')?.markAsTouched();
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitReview.emit(this.form.value as ReviewRequest);
  }
}
