export interface StoreProduct {
  id: string;
  name: string;
  description: string;
  category: string;
  price: number;
  rating: number;
  reviews: number;
  image: string;
  stockQuantity: number;
  badge?: string;
  sellerId?: number;
}

export const SAMPLE_PRODUCTS: StoreProduct[] = [
  {
    id: 'linen-throw',
    name: 'Textured cotton throw blanket',
    description: 'A soft, breathable cotton layer that adds warmth and a relaxed texture to a sofa or bed.',
    category: 'Home',
    price: 34.95,
    stockQuantity: 18,
    rating: 4.7,
    reviews: 284,
    image: 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=720&q=85',
    badge: 'Bestseller',
  },
  {
    id: 'table-lamp',
    name: 'Minimal ceramic bedside lamp',
    description: 'A simple ceramic base and warm, diffused light make this lamp an easy fit for a bedside table or reading corner.',
    category: 'Home',
    price: 48,
    stockQuantity: 12,
    rating: 4.6,
    reviews: 163,
    image: 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=720&q=85',
  },
  {
    id: 'headphones',
    name: 'Wireless over-ear headphones',
    description: 'Comfortable padded ear cups and wireless listening for work, travel, or relaxing at home.',
    category: 'Electronics',
    price: 89.99,
    stockQuantity: 7,
    rating: 4.5,
    reviews: 512,
    image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=720&q=85',
    badge: 'Popular',
  },
  {
    id: 'coffee-set',
    name: 'Stoneware coffee cup set',
    description: 'A coordinated set of stoneware cups with a tactile finish for everyday coffee and tea.',
    category: 'Kitchen',
    price: 26.5,
    stockQuantity: 24,
    rating: 4.8,
    reviews: 97,
    image: 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=720&q=85',
  },
  {
    id: 'daypack',
    name: 'Everyday canvas daypack',
    description: 'A versatile canvas bag for carrying daily essentials on a commute, a walk, or a weekend outing.',
    category: 'Outdoor',
    price: 42,
    stockQuantity: 0,
    rating: 4.4,
    reviews: 221,
    image: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=720&q=85',
  },
  {
    id: 'serving-board',
    name: 'Acacia wood serving board',
    description: 'A durable acacia wood board for preparing ingredients or serving bread, cheese, and snacks.',
    category: 'Kitchen',
    price: 31.75,
    stockQuantity: 9,
    rating: 4.7,
    reviews: 138,
    image: 'https://images.unsplash.com/photo-1603199506016-b9a594b593c0?auto=format&fit=crop&w=720&q=85',
  },
  {
    id: 'desk-organizer',
    name: 'Wood and steel desk organizer',
    description: 'Keep stationery and small desk essentials together with a compact wood and steel organizer.',
    category: 'Home',
    price: 22.99,
    stockQuantity: 4,
    rating: 4.3,
    reviews: 84,
    image: 'https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?auto=format&fit=crop&w=720&q=85',
  },
  {
    id: 'water-bottle',
    name: 'Insulated stainless bottle',
    description: 'A reusable stainless steel bottle designed to keep a drink close at hand at work or outdoors.',
    category: 'Outdoor',
    price: 19.95,
    stockQuantity: 15,
    rating: 4.6,
    reviews: 349,
    image: 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=720&q=85',
  },
];
