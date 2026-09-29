import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the Unikly brand and signed-out navigation', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.getAttribute('aria-label')).toBe('Unikly home');
    expect(compiled.querySelector('.brand img')).toBeTruthy();
    expect(compiled.querySelector('nav a')?.textContent).toContain('Sign in');
  });

  it('submits a trimmed search term through the home URL', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);
    const navigation = spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.componentInstance.searchTerm = '  ceramic lamp  ';

    await fixture.componentInstance.search();

    expect(navigation).toHaveBeenCalledWith('/?q=ceramic%20lamp');
  });
});
