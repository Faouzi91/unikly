import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SubmitProposalRequest } from '../../models/job.models';

export interface ProposalDialogData {
  jobTitle: string;
  jobBudget: number;
  jobCurrency: string;
  /** When true the dialog pre-fills and labels itself as a resubmission */
  isResubmit?: boolean;
  existingBudget?: number;
  existingCoverLetter?: string;
}

@Component({
  selector: 'app-proposal-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './proposal-dialog.component.html',
  styleUrl: './proposal-dialog.component.scss',
})
export class ProposalDialogComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);

  @Input({ required: true }) data!: ProposalDialogData;
  @Input() submitting = false;
  @Output() cancel = new EventEmitter<void>();
  @Output() submitProposal = new EventEmitter<SubmitProposalRequest>();

  readonly form: FormGroup = this.fb.group({
    proposedBudget: [null, [Validators.required, Validators.min(1), Validators.max(999999)]],
    coverLetter: ['', [Validators.required, Validators.minLength(50)]],
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data']?.currentValue) {
      this.form.patchValue({
        proposedBudget: this.data.isResubmit
          ? (this.data.existingBudget ?? this.data.jobBudget)
          : this.data.jobBudget,
        coverLetter: this.data.isResubmit ? (this.data.existingCoverLetter ?? '') : '',
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitProposal.emit(this.form.value as SubmitProposalRequest);
  }
}
