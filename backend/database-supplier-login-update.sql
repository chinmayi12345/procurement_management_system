-- Supplier login support
-- Run this only if the users table does not already contain supplier_id.
-- The project also uses spring.jpa.hibernate.ddl-auto=update, so Hibernate
-- will add the column automatically when the backend starts.

ALTER TABLE users ADD COLUMN supplier_id BIGINT NULL;

-- Example: link an existing supplier login user to supplier #1
-- UPDATE users SET supplier_id = 1, role = 'SUPPLIER' WHERE username = 'supplier1';
