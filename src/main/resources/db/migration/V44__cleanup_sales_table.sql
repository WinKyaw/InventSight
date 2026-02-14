-- V44__cleanup_sales_table.sql
-- Remove receipt-related columns from sales table
-- These columns are now in the receipts table, accessed via the sale_receipts junction table

-- Drop indexes first
DROP INDEX IF EXISTS idx_sales_receipt_type;
DROP INDEX IF EXISTS idx_sales_delivery_person;
DROP INDEX IF EXISTS idx_sales_fulfilled_by;

-- Drop receipt-related columns from sales table
ALTER TABLE sales DROP COLUMN IF EXISTS receipt_number;
ALTER TABLE sales DROP COLUMN IF EXISTS status;
ALTER TABLE sales DROP COLUMN IF EXISTS payment_method;
ALTER TABLE sales DROP COLUMN IF EXISTS fulfilled_by_user_id;
ALTER TABLE sales DROP COLUMN IF EXISTS fulfilled_at;
ALTER TABLE sales DROP COLUMN IF EXISTS receipt_type;
ALTER TABLE sales DROP COLUMN IF EXISTS delivery_person_id;
ALTER TABLE sales DROP COLUMN IF EXISTS delivery_assigned_at;
ALTER TABLE sales DROP COLUMN IF EXISTS delivered_at;
ALTER TABLE sales DROP COLUMN IF EXISTS delivery_notes;

-- Sales table now only contains sale-specific data
-- Receipt information is accessed via: sale -> sale_receipts -> receipts

-- Note: We keep customer fields (customer_id, customer_name, customer_email, customer_phone)
-- in the sales table for now as they may be used for sale-specific purposes
-- The same information is also in receipts table for receipt purposes
