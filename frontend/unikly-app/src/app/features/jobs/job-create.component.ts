import { Component, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
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
import { MatChipsModule } from '@angular/material/chips';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { JobService } from './services/job.service';

@Component({
  selector: 'app-job-create',
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
    MatChipsModule,
    MatAutocompleteModule,
    MatSnackBarModule,
  ],
  templateUrl: './job-create.component.html',
})
export class JobCreateComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly jobService = inject(JobService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroy$ = new Subject<void>();

  currencies = ['USD', 'EUR', 'XAF', 'GBP'];
  skills: string[] = [];
  skillSuggestions: string[] = [];
  submitting = false;

  skillInputControl = new FormControl('');

  form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.required]],
    budget: [null, [Validators.required, Validators.min(1)]],
    currency: ['USD', [Validators.required]],
  });

  constructor() {
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

  addSkill(event: MatAutocompleteSelectedEvent): void {
    const skill = event.option.value;
    if (!this.skills.includes(skill)) {
      this.skills.push(skill);
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.skills = this.skills.filter((s) => s !== skill);
  }

  cancel(): void {
    this.router.navigate(['/jobs']);
  }

  onSubmit(): void {
    if (this.form.invalid || this.skills.length === 0) return;

    this.submitting = true;
    const data = { ...this.form.value, skills: this.skills };

    this.jobService.createJob(data).subscribe({
      next: (job) => {
        this.snackBar.open('Job posted successfully!', 'Close', {
          duration: 3000,
        });
        this.router.navigate(['/jobs', job.id]);
      },
      error: () => {
        this.submitting = false;
      },
    });
  }
}
