import { Component, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ToastService } from '../../../core/services/toast.service';
import { JobService } from '../services/job.service';

@Component({
  selector: 'app-job-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-create.component.html',
  styleUrl: './job-create.component.scss',
})
export class JobCreateComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly jobService = inject(JobService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroy$ = new Subject<void>();

  readonly currencies = ['USD', 'EUR', 'XAF', 'GBP'];
  skills: string[] = [];
  skillSuggestions: string[] = [];
  submitting = false;

  readonly skillInputControl = new FormControl('');
  readonly form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.required, Validators.minLength(40)]],
    budget: [null, [Validators.required, Validators.min(1), Validators.max(999999)]],
    currency: ['USD', [Validators.required]],
  });

  constructor() {
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

  addSkill(skillInput?: string): void {
    const skill = (skillInput ?? this.skillInputControl.value ?? '').trim();
    if (!skill) return;
    if (!this.skills.includes(skill)) {
      this.skills.push(skill);
    }
    this.skillInputControl.setValue('');
  }

  removeSkill(skill: string): void {
    this.skills = this.skills.filter((item) => item !== skill);
  }

  cancel(): void {
    this.router.navigate(['/jobs']);
  }

  onSubmit(): void {
    if (this.form.invalid || this.skills.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const payload = { ...this.form.value, skills: this.skills };
    this.jobService.createJob(payload).subscribe({
      next: (job) => {
        this.toast.success('Project posted successfully.');
        this.router.navigate(['/jobs', job.id]);
      },
      error: () => {
        this.submitting = false;
      },
    });
  }
}
