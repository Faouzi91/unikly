import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Pure layout shell for public landing routes.
 * Each child page (LandingPageComponent, LoginComponent, RegisterComponent)
 * is responsible for its own full-page layout including navbar and footer.
 */
@Component({
  selector: 'app-landing-layout',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './landing-layout.component.html',
  styleUrl: './landing-layout.component.scss',
})
export class LandingLayoutComponent {}
