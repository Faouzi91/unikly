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

  it('computes department bento showcase cards with live starting prices and stock counts', () => {
    const fixture = TestBed.createComponent(HomePage);
    const page = fixture.componentInstance;
    const departments = page.departmentCards();

    expect(departments.length).toBe(4);
    const homeDept = departments.find((dept) => dept.category === 'Home');
    expect(homeDept?.count).toBe(3);
    expect(homeDept?.startingPrice).toBe(22.99);
  });

  it('filters by in-stock and under-$35 quick filters and resets cleanly', () => {
    const fixture = TestBed.createComponent(HomePage);
    const page = fixture.componentInstance;

    page.toggleInStockOnly();
    expect(page.filteredProducts().every((product) => product.stockQuantity > 0)).toBeTrue();

    page.toggleUnder35();
    expect(
      page.filteredProducts().every((product) => product.stockQuantity > 0 && product.price <= 35),
    ).toBeTrue();
    expect(page.hasActiveFilters()).toBeTrue();

    page.clearFilters();
    expect(page.hasActiveFilters()).toBeFalse();
    expect(page.resultCount()).toBe(8);
  });
});
