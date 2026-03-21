import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  imports: [DecimalPipe, MatIconModule],
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
