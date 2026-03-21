import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { SubmitProposalRequest } from '../../models/job.models';

export interface ProposalDialogData {
  jobTitle: string;
  jobBudget: number;
  jobCurrency: string;
}

@Component({
  selector: 'app-proposal-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './proposal-dialog.component.html',
  styleUrl: './proposal-dialog.component.scss',
})
export class ProposalDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProposalDialogComponent>);
  readonly data = inject<ProposalDialogData>(MAT_DIALOG_DATA);

  form: FormGroup = this.fb.group({
    proposedBudget: [this.data.jobBudget, [Validators.required, Validators.min(1)]],
    coverLetter: ['', [Validators.required, Validators.minLength(50)]],
  });

  submit(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value as SubmitProposalRequest);
    }
  }
}
