import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';

describe('CartService', () => {
  const storageKey = 'unikly-demo-cart-v1';

  beforeEach(() => {
    localStorage.removeItem(storageKey);
    TestBed.configureTestingModule({ providers: [CartService] });
  });

  afterEach(() => localStorage.removeItem(storageKey));

  it('increments the quantity when the same product is added twice', () => {
    const cart = TestBed.inject(CartService);

    cart.add('table-lamp');
    cart.add('table-lamp');

    expect(cart.lines()).toEqual([{ productId: 'table-lamp', quantity: 2 }]);
    expect(cart.itemCount()).toBe(2);
    expect(JSON.parse(localStorage.getItem(storageKey) ?? '[]')).toEqual(cart.lines());
  });

  it('removes an item and updates the basket count', () => {
    const cart = TestBed.inject(CartService);
    cart.add('table-lamp');
    cart.add('coffee-set');

    cart.remove('table-lamp');

    expect(cart.lines()).toEqual([{ productId: 'coffee-set', quantity: 1 }]);
    expect(cart.itemCount()).toBe(1);
  });
});
