import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'unikly-theme';
  isDarkMode = signal<boolean>(false);

  constructor() {
    const savedTheme = localStorage.getItem(this.THEME_KEY);
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    this.setTheme(savedTheme === 'dark' || (!savedTheme && prefersDark));
  }

  toggleTheme(): void {
    this.setTheme(!this.isDarkMode());
  }

  private setTheme(isDark: boolean): void {
    this.isDarkMode.set(isDark);
    if (isDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem(this.THEME_KEY, 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem(this.THEME_KEY, 'light');
    }
  }
}
