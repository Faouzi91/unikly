ALTER TABLE customer_order_items
    ADD COLUMN fulfillment_status VARCHAR(24) NOT NULL DEFAULT 'PLACED';
