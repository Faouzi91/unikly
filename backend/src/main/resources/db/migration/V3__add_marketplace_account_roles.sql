ALTER TABLE store_users DROP CONSTRAINT ck_store_users_role;
ALTER TABLE store_users ADD CONSTRAINT ck_store_users_role
    CHECK (role IN ('CUSTOMER', 'CLIENT', 'FREELANCER', 'ADMIN'));
