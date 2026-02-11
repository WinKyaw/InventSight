-- Migration: Add product_name and product_sku columns to sale_items table
-- This ensures denormalized product information is available for receipts even if product is deleted
-- Addresses issue where items show as "Unknown Item" instead of actual product names

-- Add columns if they don't exist
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS product_sku VARCHAR(100);

-- Backfill product_name and product_sku for existing records from the products table
UPDATE sale_items si
SET 
    product_name = p.name,
    product_sku = p.sku
FROM products p
WHERE si.product_id = p.id
  AND (si.product_name IS NULL OR si.product_sku IS NULL);
