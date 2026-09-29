CREATE TABLE catalog_products (
    id VARCHAR(36) PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES store_users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    price NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    image VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_catalog_products_seller_id ON catalog_products(seller_id);
