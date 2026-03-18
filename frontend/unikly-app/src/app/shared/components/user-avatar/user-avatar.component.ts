import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (avatarUrl) {
      <img
        [src]="avatarUrl"
        [alt]="displayName"
        class="rounded-full object-cover"
        [style.width.px]="size"
        [style.height.px]="size"
      />
    } @else {
      <div
        class="rounded-full flex items-center justify-center text-white font-semibold"
        [style.width.px]="size"
        [style.height.px]="size"
        [style.fontSize.px]="size * 0.4"
        [style.backgroundColor]="bgColor"
      >
        {{ initial }}
      </div>
    }
  `,
})
export class UserAvatarComponent implements OnChanges {
  @Input() avatarUrl: string | null = null;
  @Input() displayName = '';
  @Input() size = 40;

  initial = '';
  bgColor = '#6366f1';

  private readonly colors = [
    '#6366f1', '#8b5cf6', '#ec4899', '#ef4444', '#f97316',
    '#eab308', '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6',
  ];

  ngOnChanges(): void {
    this.initial = this.displayName?.charAt(0)?.toUpperCase() || '?';
    const hash = this.displayName
      .split('')
      .reduce((acc, char) => acc + char.charCodeAt(0), 0);
    this.bgColor = this.colors[hash % this.colors.length];
  }
}
