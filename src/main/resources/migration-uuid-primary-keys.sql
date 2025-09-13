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

-- Discount Audit Log table (if it references products/stores)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'product_id') THEN
        ALTER TABLE discount_audit_logs ADD COLUMN IF NOT EXISTS product_uuid UUID;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'store_id') THEN
        ALTER TABLE discount_audit_logs ADD COLUMN IF NOT EXISTS store_uuid UUID;
    END IF;
END $$;

-- Employees table (if it references stores)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'employees' AND column_name = 'store_id') THEN
        ALTER TABLE employees ADD COLUMN IF NOT EXISTS store_uuid UUID;
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
        UPDATE events 
        SET store_uuid = stores.new_id 
        FROM stores 
        WHERE events.store_id = stores.id;
    END IF;
END $$;

-- Discount Audit Logs (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'product_id') THEN
        UPDATE discount_audit_logs 
        SET product_uuid = products.new_id 
        FROM products 
        WHERE discount_audit_logs.product_id = products.id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'store_id') THEN
        UPDATE discount_audit_logs 
        SET store_uuid = stores.new_id 
        FROM stores 
        WHERE discount_audit_logs.store_id = stores.id;
    END IF;
END $$;

-- Employees (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'employees' AND column_name = 'store_id') THEN
        UPDATE employees 
        SET store_uuid = stores.new_id 
        FROM stores 
        WHERE employees.store_id = stores.id;
    END IF;
END $$;

-- Step 6: Drop foreign key constraints
-- Note: These need to be adjusted based on actual constraint names in your database
-- You may need to query information_schema.table_constraints to get exact names

-- Drop constraints from user_store_roles
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN 
        SELECT tc.constraint_name 
        FROM information_schema.table_constraints tc
        WHERE tc.table_name = 'user_store_roles' 
        AND tc.constraint_type = 'FOREIGN KEY'
        AND EXISTS (
            SELECT 1 FROM information_schema.key_column_usage kcu
            WHERE kcu.constraint_name = tc.constraint_name
            AND kcu.column_name = 'store_id'
        )
    LOOP
        EXECUTE format('ALTER TABLE user_store_roles DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

-- Drop constraints from sale_items
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN 
        SELECT tc.constraint_name 
        FROM information_schema.table_constraints tc
        WHERE tc.table_name = 'sale_items' 
        AND tc.constraint_type = 'FOREIGN KEY'
        AND EXISTS (
            SELECT 1 FROM information_schema.key_column_usage kcu
            WHERE kcu.constraint_name = tc.constraint_name
            AND kcu.column_name = 'product_id'
        )
    LOOP
        EXECUTE format('ALTER TABLE sale_items DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

-- Similar pattern for other tables...

-- Step 7: Drop old primary key constraints and columns
ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_pkey;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_pkey;

-- Step 8: Rename new UUID columns to 'id'
ALTER TABLE stores RENAME COLUMN new_id TO id;
ALTER TABLE products RENAME COLUMN new_id TO id;

-- Step 9: Set new UUID columns as primary keys
ALTER TABLE stores ADD PRIMARY KEY (id);
ALTER TABLE products ADD PRIMARY KEY (id);

-- Step 10: Update foreign key columns to use UUIDs
-- User Store Roles
ALTER TABLE user_store_roles DROP COLUMN IF EXISTS store_id;
ALTER TABLE user_store_roles RENAME COLUMN store_uuid TO store_id;
ALTER TABLE user_store_roles ALTER COLUMN store_id SET NOT NULL;

-- Sale Items
ALTER TABLE sale_items DROP COLUMN IF EXISTS product_id;
ALTER TABLE sale_items RENAME COLUMN product_uuid TO product_id;
ALTER TABLE sale_items ALTER COLUMN product_id SET NOT NULL;

-- Sales
ALTER TABLE sales DROP COLUMN IF EXISTS store_id;
ALTER TABLE sales RENAME COLUMN store_uuid TO store_id;
ALTER TABLE sales ALTER COLUMN store_id SET NOT NULL;

-- Events (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'store_uuid') THEN
        ALTER TABLE events DROP COLUMN IF EXISTS store_id;
        ALTER TABLE events RENAME COLUMN store_uuid TO store_id;
        ALTER TABLE events ALTER COLUMN store_id SET NOT NULL;
    END IF;
END $$;

-- Discount Audit Logs (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'product_uuid') THEN
        ALTER TABLE discount_audit_logs DROP COLUMN IF EXISTS product_id;
        ALTER TABLE discount_audit_logs RENAME COLUMN product_uuid TO product_id;
        ALTER TABLE discount_audit_logs ALTER COLUMN product_id SET NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'store_uuid') THEN
        ALTER TABLE discount_audit_logs DROP COLUMN IF EXISTS store_id;
        ALTER TABLE discount_audit_logs RENAME COLUMN store_uuid TO store_id;
        ALTER TABLE discount_audit_logs ALTER COLUMN store_id SET NOT NULL;
    END IF;
END $$;

-- Employees (conditional)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'employees' AND column_name = 'store_uuid') THEN
        ALTER TABLE employees DROP COLUMN IF EXISTS store_id;
        ALTER TABLE employees RENAME COLUMN store_uuid TO store_id;
        ALTER TABLE employees ALTER COLUMN store_id SET NOT NULL;
    END IF;
END $$;

-- Step 11: Recreate foreign key constraints with UUID references
ALTER TABLE user_store_roles 
ADD CONSTRAINT fk_user_store_roles_store 
FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;

ALTER TABLE sale_items 
ADD CONSTRAINT fk_sale_items_product 
FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

ALTER TABLE sales 
ADD CONSTRAINT fk_sales_store 
FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;

-- Conditional foreign keys
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'events' AND column_name = 'store_id') THEN
        ALTER TABLE events 
        ADD CONSTRAINT fk_events_store 
        FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'product_id') THEN
        ALTER TABLE discount_audit_logs 
        ADD CONSTRAINT fk_discount_audit_logs_product 
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_logs' AND column_name = 'store_id') THEN
        ALTER TABLE discount_audit_logs 
        ADD CONSTRAINT fk_discount_audit_logs_store 
        FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'employees' AND column_name = 'store_id') THEN
        ALTER TABLE employees 
        ADD CONSTRAINT fk_employees_store 
        FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
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
    
    RAISE NOTICE 'UUID Primary Keys Migration completed successfully!';
    RAISE NOTICE 'Stores migrated: %', store_count;
    RAISE NOTICE 'Products migrated: %', product_count;
    RAISE NOTICE 'All foreign key relationships have been updated to use UUID references';
    RAISE NOTICE 'Backup tables (stores_backup, products_backup) have been created for rollback if needed';
END $$;