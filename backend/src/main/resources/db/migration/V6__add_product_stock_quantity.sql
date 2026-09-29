ALTER TABLE catalog_products
    ADD COLUMN stock_quantity INTEGER NOT NULL DEFAULT 10 CHECK (stock_quantity >= 0);
