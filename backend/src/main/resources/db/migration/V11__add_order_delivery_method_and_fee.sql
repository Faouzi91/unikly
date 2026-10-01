ALTER TABLE customer_orders
    ADD COLUMN delivery_method VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN delivery_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (delivery_fee >= 0);
