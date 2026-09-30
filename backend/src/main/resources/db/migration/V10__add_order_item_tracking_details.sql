ALTER TABLE customer_order_items
    ADD COLUMN carrier_name VARCHAR(80),
    ADD COLUMN tracking_url VARCHAR(2048);
