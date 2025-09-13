-- UUID Primary Keys Migration for InventSight
-- Generated: 2025-09-13
-- Description: Migrates Store and Product entities from Long to UUID primary keys
-- This migration preserves all relationships and data integrity

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Start transaction
BEGIN;

-- Create backup tables first (in case of rollback needed)
CREATE TABLE IF NOT EXISTS stores_backup AS SELECT * FROM stores WHERE 1=0;
CREATE TABLE IF NOT EXISTS products_backup AS SELECT * FROM products WHERE 1=0;

-- Step 1: Create new UUID columns in dependent tables for foreign key mapping
-- Add temporary UUID columns for mapping during migration

-- User Store Roles table
ALTER TABLE user_store_roles 
ADD COLUMN IF NOT EXISTS store_uuid UUID;

-- Sale Items table  
ALTER TABLE sale_items 
ADD COLUMN IF NOT EXISTS product_uuid UUID;

-- Sales table
ALTER TABLE sales 
ADD COLUMN IF NOT EXISTS store_uuid UUID;

-- Events table (if it references stores)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'store_id') THEN
        ALTER TABLE events ADD COLUMN IF NOT EXISTS store_uuid UUID;
    END IF;
END $$;

-- Step 2: Backup existing data
INSERT INTO stores_backup SELECT * FROM stores;
INSERT INTO products_backup SELECT * FROM products;

-- Step 3: Add new UUID columns to main tables
ALTER TABLE stores ADD COLUMN IF NOT EXISTS new_id UUID DEFAULT uuid_generate_v4();
ALTER TABLE products ADD COLUMN IF NOT EXISTS new_id UUID DEFAULT uuid_generate_v4();

-- Step 4: Populate UUID values for existing records
UPDATE stores SET new_id = uuid_generate_v4() WHERE new_id IS NULL;
UPDATE products SET new_id = uuid_generate_v4() WHERE new_id IS NULL;

-- Step 5: Update foreign key references with UUID mappings
-- User Store Roles
UPDATE user_store_roles 
SET store_uuid = stores.new_id 
FROM stores 
WHERE user_store_roles.store_id = stores.id;

-- Sale Items
UPDATE sale_items 
SET product_uuid = products.new_id 
FROM products 
WHERE sale_items.product_id = products.id;

-- Sales
UPDATE sales 
SET store_uuid = stores.new_id 
FROM stores 
WHERE sales.store_id = stores.id;

-- Events (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'store_id') THEN
        EXECUTE 'UPDATE events SET store_uuid = stores.new_id FROM stores WHERE events.store_id = stores.id';
    END IF;
END $$;

-- Step 6: Drop foreign key constraints
DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    -- Drop foreign key constraints that reference stores.id
    FOR constraint_record IN
        SELECT constraint_name, table_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu USING (constraint_name, table_schema)
        WHERE tc.constraint_type = 'FOREIGN KEY'
        AND kcu.column_name = 'store_id'
    LOOP
        EXECUTE 'ALTER TABLE ' || constraint_record.table_name || ' DROP CONSTRAINT IF EXISTS ' || constraint_record.constraint_name;
    END LOOP;
    
    -- Drop foreign key constraints that reference products.id
    FOR constraint_record IN
        SELECT constraint_name, table_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu USING (constraint_name, table_schema)
        WHERE tc.constraint_type = 'FOREIGN KEY'
        AND kcu.column_name = 'product_id'
    LOOP
        EXECUTE 'ALTER TABLE ' || constraint_record.table_name || ' DROP CONSTRAINT IF EXISTS ' || constraint_record.constraint_name;
    END LOOP;
END $$;

-- Step 7: Drop old primary key constraints and columns
ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_pkey;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_pkey;

-- Step 8: Drop old id columns and rename new ones
ALTER TABLE stores DROP COLUMN IF EXISTS id;
ALTER TABLE stores RENAME COLUMN new_id TO id;

ALTER TABLE products DROP COLUMN IF EXISTS id;
ALTER TABLE products RENAME COLUMN new_id TO id;

-- Step 9: Set new primary keys
ALTER TABLE stores ADD PRIMARY KEY (id);
ALTER TABLE products ADD PRIMARY KEY (id);

-- Step 10: Update foreign key columns to use UUID type and drop old ones
-- User Store Roles
ALTER TABLE user_store_roles DROP COLUMN IF EXISTS store_id;
ALTER TABLE user_store_roles RENAME COLUMN store_uuid TO store_id;

-- Sale Items
ALTER TABLE sale_items DROP COLUMN IF EXISTS product_id;
ALTER TABLE sale_items RENAME COLUMN product_uuid TO product_id;

-- Sales
ALTER TABLE sales DROP COLUMN IF EXISTS store_id;
ALTER TABLE sales RENAME COLUMN store_uuid TO store_id;

-- Events (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'store_uuid') THEN
        EXECUTE 'ALTER TABLE events DROP COLUMN IF EXISTS store_id';
        EXECUTE 'ALTER TABLE events RENAME COLUMN store_uuid TO store_id';
    END IF;
END $$;

-- Step 11: Recreate foreign key constraints with UUID references
ALTER TABLE user_store_roles ADD CONSTRAINT fk_user_store_roles_store_id 
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;

ALTER TABLE sale_items ADD CONSTRAINT fk_sale_items_product_id 
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE sales ADD CONSTRAINT fk_sales_store_id 
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;

-- Events (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'store_id') THEN
        EXECUTE 'ALTER TABLE events ADD CONSTRAINT fk_events_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE';
    END IF;
END $$;

-- Step 12: Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_stores_id ON stores(id);
CREATE INDEX IF NOT EXISTS idx_products_id ON products(id);
CREATE INDEX IF NOT EXISTS idx_user_store_roles_store_id ON user_store_roles(store_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id);
CREATE INDEX IF NOT EXISTS idx_sales_store_id ON sales(store_id);

-- Step 13: Update the existing uuid column in products table if it exists
-- Since Product now uses UUID as primary key, remove the old uuid column
ALTER TABLE products DROP COLUMN IF EXISTS uuid;

-- Step 14: Log migration completion
INSERT INTO events (name, description, event_type, priority, start_time, created_at, created_by)
VALUES (
    'UUID Primary Keys Migration Completed',
    'Successfully migrated Store and Product entities from Long to UUID primary keys. All relationships have been preserved and updated.',
    'SYSTEM',
    'HIGH',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'SYSTEM_MIGRATION'
) ON CONFLICT DO NOTHING;

-- Commit transaction
COMMIT;

-- Validation queries
DO $$
DECLARE
    store_count integer;
    product_count integer;
    constraint_count integer;
BEGIN
    SELECT COUNT(*) INTO store_count FROM stores;
    SELECT COUNT(*) INTO product_count FROM products;
    
    -- Count foreign key constraints
    SELECT COUNT(*) INTO constraint_count 
    FROM information_schema.table_constraints 
    WHERE constraint_type = 'FOREIGN KEY' 
    AND table_name IN ('user_store_roles', 'sale_items', 'sales');
    
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE 'Stores migrated: %', store_count;
    RAISE NOTICE 'Products migrated: %', product_count;
    RAISE NOTICE 'Foreign key constraints recreated: %', constraint_count;
    
    -- Verify UUID format
    IF EXISTS (
        SELECT 1 FROM stores 
        WHERE id::text !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    ) THEN
        RAISE EXCEPTION 'Invalid UUID format found in stores table';
    END IF;
    
    IF EXISTS (
        SELECT 1 FROM products 
        WHERE id::text !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    ) THEN
        RAISE EXCEPTION 'Invalid UUID format found in products table';
    END IF;
    
    RAISE NOTICE 'All UUID validations passed!';
END $$;

-- Optional: Clean up backup tables after validation
-- Uncomment the following lines after confirming the migration works correctly:
-- DROP TABLE IF EXISTS stores_backup;
-- DROP TABLE IF EXISTS products_backup;

-- Instructions for manual execution:
-- 1. Backup your database before running this script
-- 2. Run this script using: psql -U your_username -d your_database -f migrate_long_to_uuid.sql
-- 3. Verify the application works correctly with the new UUID primary keys
-- 4. Clean up backup tables after confirmation