ALTER TABLE catalog_products ALTER COLUMN seller_id DROP NOT NULL;

INSERT INTO catalog_products (
    id, seller_id, name, description, category, price, image, stock_quantity, created_at, updated_at
) VALUES
    ('linen-throw', NULL, 'Textured cotton throw blanket', 'A soft, breathable cotton layer that adds warmth and a relaxed texture to a sofa or bed.', 'Home', 34.95, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=720&q=85', 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('table-lamp', NULL, 'Minimal ceramic bedside lamp', 'A simple ceramic base and warm, diffused light make this lamp an easy fit for a bedside table or reading corner.', 'Home', 48.00, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=720&q=85', 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('headphones', NULL, 'Wireless over-ear headphones', 'Comfortable padded ear cups and wireless listening for work, travel, or relaxing at home.', 'Electronics', 89.99, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=720&q=85', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('coffee-set', NULL, 'Stoneware coffee cup set', 'A coordinated set of stoneware cups with a tactile finish for everyday coffee and tea.', 'Kitchen', 26.50, 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=720&q=85', 24, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('daypack', NULL, 'Everyday canvas daypack', 'A versatile canvas bag for carrying daily essentials on a commute, a walk, or a weekend outing.', 'Outdoor', 42.00, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=720&q=85', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('serving-board', NULL, 'Acacia wood serving board', 'A durable acacia wood board for preparing ingredients or serving bread, cheese, and snacks.', 'Kitchen', 31.75, 'https://images.unsplash.com/photo-1603199506016-b9a594b593c0?auto=format&fit=crop&w=720&q=85', 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('desk-organizer', NULL, 'Wood and steel desk organizer', 'Keep stationery and small desk essentials together with a compact wood and steel organizer.', 'Home', 22.99, 'https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?auto=format&fit=crop&w=720&q=85', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('water-bottle', NULL, 'Insulated stainless bottle', 'A reusable stainless steel bottle designed to keep a drink close at hand at work or outdoors.', 'Outdoor', 19.95, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=720&q=85', 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
