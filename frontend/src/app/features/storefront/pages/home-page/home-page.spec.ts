import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HomePage } from './home-page';

describe('HomePage product search', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('filters products by a trimmed, case-insensitive search query', () => {
    const fixture = TestBed.createComponent(HomePage);
    const page = fixture.componentInstance;
    page.query.set('  LAMP  ');

    expect(page.filteredProducts().map((product) => product.id)).toEqual(['table-lamp']);
  });

  it('combines category and text filters', () => {
    const fixture = TestBed.createComponent(HomePage);
    const page = fixture.componentInstance;
    page.activeCategory.set('Kitchen');
    page.query.set('coffee');

    expect(page.filteredProducts().map((product) => product.id)).toEqual(['coffee-set']);
  });

  it('reports the visible product count for each category', () => {
    const fixture = TestBed.createComponent(HomePage);
    const page = fixture.componentInstance;

    expect(page.categoryCount('All')).toBe(8);
    expect(page.categoryCount('Home')).toBe(3);

    page.activeCategory.set('Home');
    expect(page.resultCount()).toBe(3);
  });
});
