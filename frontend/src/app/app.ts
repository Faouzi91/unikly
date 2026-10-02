import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { LucideSearch, LucideShoppingCart, LucideUser } from '@lucide/angular';
import { AuthService } from '@core/identity/auth.service';
import { CartService } from '@core/cart/cart.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, FormsModule, LucideSearch, LucideShoppingCart, LucideUser],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
  private readonly router = inject(Router);
  searchTerm = '';
  readonly currentYear = new Date().getFullYear();

  async search(): Promise<void> {
    await this.router.navigateByUrl('/?q=' + encodeURIComponent(this.searchTerm.trim()));
  }

  async signOut(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
