-- Fix broken Unsplash image URL for aluminum-pencil-set
UPDATE catalog_products
SET image = 'https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?auto=format&fit=crop&w=720&q=85',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'aluminum-pencil-set';
