import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-skill-chips',
  standalone: true,
  imports: [CommonModule, MatChipsModule, MatIconModule],
  template: `
    <div class="flex flex-wrap gap-1">
      @for (skill of skills; track skill) {
        <mat-chip-row
          [removable]="editable"
          (removed)="editable && onRemove(skill)"
        >
          {{ skill }}
          @if (editable) {
            <button matChipRemove>
              <mat-icon>cancel</mat-icon>
            </button>
          }
        </mat-chip-row>
      }
      @if (skills.length === 0) {
        <span class="text-sm text-gray-400">No skills listed</span>
      }
    </div>
  `,
})
export class SkillChipsComponent {
  @Input() skills: string[] = [];
  @Input() editable = false;
  @Output() skillRemoved = new EventEmitter<string>();

  onRemove(skill: string): void {
    this.skillRemoved.emit(skill);
  }
}
