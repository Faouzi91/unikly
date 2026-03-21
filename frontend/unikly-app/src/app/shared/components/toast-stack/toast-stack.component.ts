import { Component, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { ToastService, ToastVariant } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-stack',
  standalone: true,
  imports: [NgClass],
  templateUrl: './toast-stack.component.html',
})
export class ToastStackComponent {
  readonly toastService = inject(ToastService);

  dismiss(id: string): void {
    this.toastService.dismiss(id);
  }

  classesFor(variant: ToastVariant): string {
    if (variant === 'success') return 'border-emerald-200 bg-emerald-50 text-emerald-800';
    if (variant === 'warning') return 'border-amber-200 bg-amber-50 text-amber-800';
    if (variant === 'error') return 'border-rose-200 bg-rose-50 text-rose-800';
    return 'border-sky-200 bg-sky-50 text-sky-800';
  }
}
