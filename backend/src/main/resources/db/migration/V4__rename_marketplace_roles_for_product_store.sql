ALTER TABLE store_users DROP CONSTRAINT ck_store_users_role;
UPDATE store_users
SET role = CASE role
    WHEN 'CUSTOMER' THEN 'BUYER'
    WHEN 'CLIENT' THEN 'BUYER'
    WHEN 'FREELANCER' THEN 'SELLER'
    ELSE role
END;
ALTER TABLE store_users ADD CONSTRAINT ck_store_users_role
    CHECK (role IN ('BUYER', 'SELLER', 'ADMIN'));
