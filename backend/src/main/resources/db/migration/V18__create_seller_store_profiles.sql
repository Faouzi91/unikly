CREATE TABLE seller_store_profiles (
    seller_id BIGINT PRIMARY KEY REFERENCES store_users(id) ON DELETE CASCADE,
    store_name VARCHAR(80) NOT NULL,
    headline VARCHAR(140),
    description VARCHAR(600),
    location VARCHAR(80),
    support_email VARCHAR(160),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
