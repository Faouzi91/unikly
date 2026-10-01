-- Assign unowned catalog products to seller@unikly.local if the dev account exists
UPDATE catalog_products
SET seller_id = (SELECT id FROM store_users WHERE email = 'seller@unikly.local' LIMIT 1)
WHERE seller_id IS NULL
  AND EXISTS (SELECT 1 FROM store_users WHERE email = 'seller@unikly.local');
