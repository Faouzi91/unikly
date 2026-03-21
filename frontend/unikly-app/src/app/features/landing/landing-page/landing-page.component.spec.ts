import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { LandingPageComponent } from './landing-page.component';
import { ThemeService } from '../../../core/services/theme.service';

describe('LandingPageComponent', () => {
  let component: LandingPageComponent;
  let fixture: ComponentFixture<LandingPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingPageComponent, RouterTestingModule],
      providers: [ThemeService],
    }).compileComponents();

    fixture = TestBed.createComponent(LandingPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose the skills list', () => {
    expect(component.skills.length).toBeGreaterThan(0);
  });

  it('should expose the stats list', () => {
    expect(component.stats.length).toBe(4);
  });

  it('should expose ai matches for the hero widget', () => {
    expect(component.aiMatches.length).toBeGreaterThan(0);
  });
});
