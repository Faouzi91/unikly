import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-skill-chips',
  standalone: true,
  imports: [],
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
