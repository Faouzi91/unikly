import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="inline-flex items-center gap-0.5">
      @for (star of stars; track star) {
        <mat-icon
          [class]="getStarClass(star)"
          [class.cursor-pointer]="interactive"
          (click)="interactive && onStarClick(star)"
          (mouseenter)="interactive && onStarHover(star)"
          (mouseleave)="interactive && onStarLeave()"
        >
          {{ getStarIcon(star) }}
        </mat-icon>
      }
      @if (showValue) {
        <span class="ml-1 text-sm text-gray-600">{{ rating | number: '1.1-1' }}</span>
      }
    </div>
  `,
  styles: `
    .star-filled { color: #f59e0b; }
    .star-half { color: #f59e0b; }
    .star-empty { color: #d1d5db; }
  `,
})
export class StarRatingComponent {
  @Input() rating = 0;
  @Input() interactive = false;
  @Input() showValue = false;
  @Output() ratingChange = new EventEmitter<number>();

  stars = [1, 2, 3, 4, 5];
  hoveredStar = 0;

  getStarIcon(star: number): string {
    const value = this.hoveredStar || this.rating;
    if (star <= Math.floor(value)) return 'star';
    if (star - 0.5 <= value) return 'star_half';
    return 'star_border';
  }

  getStarClass(star: number): string {
    const value = this.hoveredStar || this.rating;
    if (star <= Math.floor(value)) return 'star-filled';
    if (star - 0.5 <= value) return 'star-half';
    return 'star-empty';
  }

  onStarClick(star: number): void {
    this.rating = star;
    this.ratingChange.emit(star);
  }

  onStarHover(star: number): void {
    this.hoveredStar = star;
  }

  onStarLeave(): void {
    this.hoveredStar = 0;
  }
}
