import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@core/identity/auth.service';
import { CartService } from './cart.service';

describe('CartService', () => {
  const storageKey = 'unikly-demo-cart-v1';

  beforeEach(() => {
    localStorage.removeItem(storageKey);
    TestBed.configureTestingModule({
      providers: [
        CartService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            user: signal(null),
          },
        },
      ],
    });
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

  it('updates quantity directly and caps at available stock', () => {
    const cart = TestBed.inject(CartService);
    cart.add('table-lamp');

    cart.updateQuantity('table-lamp', 4, 10);
    expect(cart.lines()).toEqual([{ productId: 'table-lamp', quantity: 4 }]);
    expect(cart.itemCount()).toBe(4);

    // Caps at available quantity
    cart.updateQuantity('table-lamp', 15, 5);
    expect(cart.lines()).toEqual([{ productId: 'table-lamp', quantity: 5 }]);

    // Removes if quantity < 1
    cart.updateQuantity('table-lamp', 0);
    expect(cart.lines()).toEqual([]);
  });
});
