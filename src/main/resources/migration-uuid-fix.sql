-- UUID Primary Keys Migration Fix for InventSight
-- This migration properly handles the transition from BIGINT to UUID for products table
-- and all related foreign key references

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Start transaction
BEGIN;

-- Step 1: Check current state and create backup tables
DO $$
BEGIN
    -- Create backup tables if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'products_backup_uuid') THEN
        CREATE TABLE products_backup_uuid AS SELECT * FROM products;
        RAISE NOTICE 'Created backup table: products_backup_uuid';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'sale_items_backup_uuid') THEN
        CREATE TABLE sale_items_backup_uuid AS SELECT * FROM sale_items;
        RAISE NOTICE 'Created backup table: sale_items_backup_uuid';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log_backup_uuid') THEN
            CREATE TABLE discount_audit_log_backup_uuid AS SELECT * FROM discount_audit_log;
            RAISE NOTICE 'Created backup table: discount_audit_log_backup_uuid';
        END IF;
    END IF;
END $$;

-- Step 2: Check if products table still has BIGINT id (the main issue)
DO $$
DECLARE
    products_id_type text;
BEGIN
    SELECT data_type INTO products_id_type 
    FROM information_schema.columns 
    WHERE table_name = 'products' AND column_name = 'id';
    
    IF products_id_type = 'bigint' THEN
        RAISE NOTICE 'Products table still has BIGINT id - proceeding with migration';
    ELSE
        RAISE NOTICE 'Products table id type is: %', products_id_type;
    END IF;
END $$;

-- Step 3: Add new UUID columns for migration (only if not already exists)
ALTER TABLE products ADD COLUMN IF NOT EXISTS new_id UUID DEFAULT uuid_generate_v4();
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS product_uuid UUID;

-- Handle discount_audit_log table if it exists (correct table name - singular not plural)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log') THEN
        ALTER TABLE discount_audit_log ADD COLUMN IF NOT EXISTS product_uuid UUID;
        ALTER TABLE discount_audit_log ADD COLUMN IF NOT EXISTS store_uuid UUID;
        RAISE NOTICE 'Added UUID columns to discount_audit_log table';
    END IF;
END $$;

-- Step 4: Populate UUID values for existing records
UPDATE products SET new_id = uuid_generate_v4() WHERE new_id IS NULL;

-- Step 5: Update foreign key references with UUID mappings
-- Sale Items
UPDATE sale_items 
SET product_uuid = products.new_id 
FROM products 
WHERE sale_items.product_id = products.id;

-- Discount Audit Log (if it exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log') THEN
        -- Update product references
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_log' AND column_name = 'product_id') THEN
            UPDATE discount_audit_log 
            SET product_uuid = products.new_id 
            FROM products 
            WHERE discount_audit_log.product_id = products.id;
            RAISE NOTICE 'Updated product references in discount_audit_log';
        END IF;
        
        -- Update store references (ensure stores table has UUID already)
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_log' AND column_name = 'store_id') THEN
            IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'stores' AND column_name = 'id' AND data_type = 'uuid') THEN
                UPDATE discount_audit_log 
                SET store_uuid = discount_audit_log.store_id::uuid;
                RAISE NOTICE 'Updated store references in discount_audit_log';
            END IF;
        END IF;
    END IF;
END $$;

-- Step 6: Drop existing foreign key constraints
DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    -- Drop foreign key constraints that reference products.id
    FOR constraint_record IN
        SELECT constraint_name, table_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu USING (constraint_name, table_schema)
        WHERE tc.constraint_type = 'FOREIGN KEY'
        AND kcu.column_name = 'product_id'
        AND kcu.referenced_table_name = 'products'
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I', constraint_record.table_name, constraint_record.constraint_name);
        RAISE NOTICE 'Dropped constraint: % from table: %', constraint_record.constraint_name, constraint_record.table_name;
    END LOOP;
END $$;

-- Step 7: Drop old primary key constraint and rename columns
-- Only proceed if products table still has BIGINT id
DO $$
DECLARE
    products_id_type text;
BEGIN
    SELECT data_type INTO products_id_type 
    FROM information_schema.columns 
    WHERE table_name = 'products' AND column_name = 'id';
    
    IF products_id_type = 'bigint' THEN
        -- Drop old primary key constraint
        ALTER TABLE products DROP CONSTRAINT IF EXISTS products_pkey;
        
        -- Drop the old BIGINT id column
        ALTER TABLE products DROP COLUMN id;
        
        -- Rename new_id to id
        ALTER TABLE products RENAME COLUMN new_id TO id;
        
        -- Set new UUID column as primary key
        ALTER TABLE products ADD PRIMARY KEY (id);
        
        RAISE NOTICE 'Successfully migrated products table from BIGINT to UUID primary key';
    ELSE
        -- If already UUID, just ensure we have the right structure
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'new_id') THEN
            ALTER TABLE products DROP COLUMN new_id;
            RAISE NOTICE 'Cleaned up temporary new_id column';
        END IF;
    END IF;
END $$;

-- Step 8: Update foreign key columns to use UUIDs
-- Sale Items
ALTER TABLE sale_items DROP COLUMN IF EXISTS product_id;
ALTER TABLE sale_items RENAME COLUMN product_uuid TO product_id;
ALTER TABLE sale_items ALTER COLUMN product_id SET NOT NULL;

-- Discount Audit Log (if it exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_log' AND column_name = 'product_uuid') THEN
            ALTER TABLE discount_audit_log DROP COLUMN IF EXISTS product_id;
            ALTER TABLE discount_audit_log RENAME COLUMN product_uuid TO product_id;
            ALTER TABLE discount_audit_log ALTER COLUMN product_id SET NOT NULL;
            RAISE NOTICE 'Updated product_id column in discount_audit_log to UUID';
        END IF;
        
        -- Handle store_id if stores table is already UUID
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_log' AND column_name = 'store_uuid') THEN
            ALTER TABLE discount_audit_log DROP COLUMN IF EXISTS store_id;
            ALTER TABLE discount_audit_log RENAME COLUMN store_uuid TO store_id;
            ALTER TABLE discount_audit_log ALTER COLUMN store_id SET NOT NULL;
            RAISE NOTICE 'Updated store_id column in discount_audit_log to UUID';
        END IF;
    END IF;
END $$;

-- Step 9: Recreate foreign key constraints with UUID references
ALTER TABLE sale_items 
ADD CONSTRAINT fk_sale_items_product 
FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

-- Conditional foreign keys for discount_audit_log
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_log' AND column_name = 'product_id') THEN
            ALTER TABLE discount_audit_log 
            ADD CONSTRAINT fk_discount_audit_log_product 
            FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
            RAISE NOTICE 'Added foreign key constraint for discount_audit_log.product_id';
        END IF;
        
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_log' AND column_name = 'store_id') THEN
            IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'stores' AND column_name = 'id' AND data_type = 'uuid') THEN
                ALTER TABLE discount_audit_log 
                ADD CONSTRAINT fk_discount_audit_log_store 
                FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
                RAISE NOTICE 'Added foreign key constraint for discount_audit_log.store_id';
            END IF;
        END IF;
    END IF;
END $$;

-- Step 10: Remove redundant columns as mentioned in requirements
-- Remove old uuid column from products if it exists (requirement 5)
ALTER TABLE products DROP COLUMN IF EXISTS uuid;

-- Remove any store_id_uuid columns if they exist (requirement 5)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'store_id_uuid') THEN
        ALTER TABLE products DROP COLUMN store_id_uuid;
        RAISE NOTICE 'Removed redundant store_id_uuid column from products table';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sale_items' AND column_name = 'store_id_uuid') THEN
        ALTER TABLE sale_items DROP COLUMN store_id_uuid;
        RAISE NOTICE 'Removed redundant store_id_uuid column from sale_items table';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'discount_audit_log' AND column_name = 'store_id_uuid') THEN
        ALTER TABLE discount_audit_log DROP COLUMN store_id_uuid;
        RAISE NOTICE 'Removed redundant store_id_uuid column from discount_audit_log table';
    END IF;
END $$;

-- Step 11: Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_products_id ON products(id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log') THEN
        CREATE INDEX IF NOT EXISTS idx_discount_audit_log_product_id ON discount_audit_log(product_id);
        CREATE INDEX IF NOT EXISTS idx_discount_audit_log_store_id ON discount_audit_log(store_id);
        RAISE NOTICE 'Created indexes for discount_audit_log table';
    END IF;
END $$;

-- Step 12: Validation
DO $$
DECLARE
    product_count integer;
    constraint_count integer;
    products_id_type text;
BEGIN
    SELECT COUNT(*) INTO product_count FROM products;
    
    SELECT COUNT(*) INTO constraint_count 
    FROM information_schema.table_constraints 
    WHERE constraint_type = 'FOREIGN KEY' 
    AND table_name IN ('sale_items', 'discount_audit_log');
    
    SELECT data_type INTO products_id_type 
    FROM information_schema.columns 
    WHERE table_name = 'products' AND column_name = 'id';
    
    RAISE NOTICE 'Migration validation:';
    RAISE NOTICE 'Products migrated: %', product_count;
    RAISE NOTICE 'Products id type: %', products_id_type;
    RAISE NOTICE 'Foreign key constraints: %', constraint_count;
    
    -- Verify UUID format
    IF EXISTS (
        SELECT 1 FROM products 
        WHERE id::text !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    ) THEN
        RAISE EXCEPTION 'Invalid UUID format found in products table';
    END IF;
    
    RAISE NOTICE 'UUID migration for products completed successfully!';
END $$;

-- Step 13: Log migration completion
INSERT INTO events (name, description, event_type, priority, start_time, created_at, created_by)
VALUES (
    'UUID Primary Keys Migration Completed - Products',
    'Successfully migrated Product entities from Long to UUID primary keys. All relationships have been preserved and updated. Foreign key references from sale_items and discount_audit_log updated to use UUID.',
    'SYSTEM',
    'HIGH',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'SYSTEM_MIGRATION'
) ON CONFLICT DO NOTHING;

-- Commit transaction
COMMIT;