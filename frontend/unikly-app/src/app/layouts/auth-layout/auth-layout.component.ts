import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet, MatCardModule],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gray-100">
      <mat-card class="w-full max-w-md p-8">
        <router-outlet />
      </mat-card>
    </div>
  `,
})
export class AuthLayoutComponent {}
