-- Change SKU unique constraint from global to per-location
-- This allows the same SKU to exist in multiple stores/warehouses as separate products

BEGIN;

-- Drop the existing global unique constraint on SKU
-- Constraint name may vary - try common variations
DO $$
BEGIN
    -- Try to drop constraint by common names
    ALTER TABLE products DROP CONSTRAINT IF EXISTS ukfhmd06dsmj6k0n90swsh8ie9g;
    ALTER TABLE products DROP CONSTRAINT IF EXISTS products_sku_key;
    ALTER TABLE products DROP CONSTRAINT IF EXISTS uk_products_sku;
EXCEPTION
    WHEN undefined_object THEN
        RAISE NOTICE 'SKU unique constraint not found, continuing...';
END $$;

-- Also drop any unique index on SKU column
DROP INDEX IF EXISTS idx_products_sku_unique;

-- Create partial unique indexes: SKU must be unique within each store
-- This allows Store A and Store B to both have products with SKU "12345"
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku_store_unique 
ON products(sku, store_id) 
WHERE store_id IS NOT NULL AND sku IS NOT NULL;

-- Create partial unique indexes: SKU must be unique within each warehouse
-- This allows Warehouse A and Warehouse B to both have products with SKU "12345"
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku_warehouse_unique 
ON products(sku, warehouse_id) 
WHERE warehouse_id IS NOT NULL AND sku IS NOT NULL;

-- Add helpful comments
COMMENT ON INDEX idx_products_sku_store_unique IS 
'Ensures SKU is unique within each store (same SKU can exist in different stores)';

COMMENT ON INDEX idx_products_sku_warehouse_unique IS 
'Ensures SKU is unique within each warehouse (same SKU can exist in different warehouses)';

COMMENT ON COLUMN products.sku IS 
'Product SKU - must be unique within the same store or warehouse, but can be reused across different locations';

COMMIT;
