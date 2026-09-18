-- Procurement payment update
-- JPA ddl-auto=update will create these columns automatically on application start.
-- Run these manually only if you are managing the schema yourself.

ALTER TABLE suppliers ADD COLUMN upi_id VARCHAR(255);

ALTER TABLE payments ADD COLUMN payment_method VARCHAR(50);
ALTER TABLE payments ADD COLUMN card_last4 VARCHAR(4);
ALTER TABLE payments ADD COLUMN transaction_reference VARCHAR(255);

-- Product -> supplier is already present in this project:
-- products.supplier_id
-- Therefore a laptop/model can be assigned to Supplier #1, another model to Supplier #2, etc.
