CREATE TABLE customer_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES store_users(id) ON DELETE CASCADE,
    phone_number VARCHAR(32),
    address_line_1 VARCHAR(120),
    address_line_2 VARCHAR(120),
    city VARCHAR(80),
    region VARCHAR(80),
    postal_code VARCHAR(24),
    country_code VARCHAR(2),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);