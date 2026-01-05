-- Make products.store_id nullable to support warehouse-only products
-- Products can now belong to either a store OR a warehouse (or both)

BEGIN;

-- Remove NOT NULL constraint from store_id (if it exists)
-- This migration is idempotent - it won't fail if constraint is already removed
DO $$
BEGIN
    -- PostgreSQL doesn't have IF EXISTS for DROP NOT NULL, so we try-catch it
    BEGIN
        ALTER TABLE products ALTER COLUMN store_id DROP NOT NULL;
    EXCEPTION
        WHEN undefined_column THEN
            -- Column doesn't exist, ignore
            NULL;
        WHEN others THEN
            -- Constraint may already be removed or other error, continue
            NULL;
    END;
END $$;

-- Add check constraint to ensure product belongs to at least one location
-- (Either store_id OR warehouse_id must be set, but not necessarily both)
-- Use IF NOT EXISTS equivalent by checking constraint name
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_product_has_location'
    ) THEN
        ALTER TABLE products
        ADD CONSTRAINT chk_product_has_location
        CHECK (store_id IS NOT NULL OR warehouse_id IS NOT NULL);
    END IF;
END $$;

-- Add helpful comments
COMMENT ON COLUMN products.store_id IS 'Store that owns this product (NULL if warehouse-only product)';
COMMENT ON COLUMN products.warehouse_id IS 'Warehouse that owns this product (NULL if store-only product)';

-- Create index for better query performance on warehouse products
CREATE INDEX IF NOT EXISTS idx_products_warehouse_id_active 
ON products(warehouse_id) 
WHERE warehouse_id IS NOT NULL AND is_active = true;

-- Create index for better query performance on store products  
CREATE INDEX IF NOT EXISTS idx_products_store_id_active 
ON products(store_id) 
WHERE store_id IS NOT NULL AND is_active = true;

COMMIT;
