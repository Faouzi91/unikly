import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-skill-chips',
  standalone: true,
  imports: [MatChipsModule, MatIconModule],
  templateUrl: './skill-chips.component.html',
  styleUrl: './skill-chips.component.scss',
})
export class SkillChipsComponent {
  @Input() skills: string[] = [];
  @Input() editable = false;
  @Output() skillRemoved = new EventEmitter<string>();

  onRemove(skill: string): void {
    this.skillRemoved.emit(skill);
  }
}
