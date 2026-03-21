import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './star-rating.component.html',
  styleUrl: './star-rating.component.scss',
})
export class StarRatingComponent {
  @Input() rating = 0;
  @Input() interactive = false;
  @Input() showValue = false;
  @Output() ratingChange = new EventEmitter<number>();

  stars = [1, 2, 3, 4, 5];
  hoveredStar = 0;

  getStarState(star: number): 'full' | 'half' | 'empty' {
    const value = this.hoveredStar || this.rating;
    if (star <= Math.floor(value)) return 'full';
    if (star - 0.5 <= value) return 'half';
    return 'empty';
  }

  getStarClass(star: number): string {
    const state = this.getStarState(star);
    if (state === 'full') return 'star-filled';
    if (state === 'half') return 'star-half';
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
