-- Add index on seller_id in customer_order_items for efficient seller order lookups
CREATE INDEX IF NOT EXISTS ix_customer_order_items_seller_id ON customer_order_items(seller_id);
