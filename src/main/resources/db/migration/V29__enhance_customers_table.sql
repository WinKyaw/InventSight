-- V29__enhance_customers_table.sql
-- Add location tracking, browsing history, and purchase tracking to customers table

-- Add location tracking
ALTER TABLE customers 
ADD COLUMN latitude DECIMAL(10, 7),
ADD COLUMN longitude DECIMAL(10, 7);

-- Add browsing history
ALTER TABLE customers 
ADD COLUMN recently_browsed_items TEXT,
ADD COLUMN last_browsed_at TIMESTAMP;

-- Add purchase tracking
ALTER TABLE customers 
ADD COLUMN last_purchase_date TIMESTAMP;

-- Add indexes
CREATE INDEX idx_customers_location ON customers(latitude, longitude);
CREATE INDEX idx_customers_last_purchase ON customers(last_purchase_date);
