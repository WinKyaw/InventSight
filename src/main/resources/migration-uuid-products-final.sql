-- UUID Primary Keys Migration for Products Table - Final Version
-- This migration robustly handles the transition from BIGINT to UUID for products table
-- and all related foreign key references, regardless of existing constraint names
--
-- Author: System Migration
-- Date: 2025-01-13
-- Description: Fixes foreign key constraint discovery and handles all edge cases

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Start transaction
BEGIN;

-- Log migration start
DO $$
BEGIN
    RAISE NOTICE 'Starting UUID migration for products table at %', CURRENT_TIMESTAMP;
END $$;

-- Step 1: Create backup tables
DO $$
BEGIN
    -- Create backup tables if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'products_backup_uuid_final') THEN
        CREATE TABLE products_backup_uuid_final AS SELECT * FROM products;
        RAISE NOTICE 'Created backup table: products_backup_uuid_final';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'sale_items_backup_uuid_final') THEN
        CREATE TABLE sale_items_backup_uuid_final AS SELECT * FROM sale_items;
        RAISE NOTICE 'Created backup table: sale_items_backup_uuid_final';
    END IF;
    
    -- Check if discount_audit_log table exists (note: singular name)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'discount_audit_log_backup_uuid_final') THEN
            CREATE TABLE discount_audit_log_backup_uuid_final AS SELECT * FROM discount_audit_log;
            RAISE NOTICE 'Created backup table: discount_audit_log_backup_uuid_final';
        END IF;
    END IF;
END $$;

-- Step 2: Check current state of products table
DO $$
DECLARE
    products_id_type text;
    products_column_count integer;
BEGIN
    -- Check if products table exists and get id column type
    SELECT data_type INTO products_id_type 
    FROM information_schema.columns 
    WHERE table_schema = CURRENT_SCHEMA() 
    AND table_name = 'products' 
    AND column_name = 'id';
    
    SELECT COUNT(*) INTO products_column_count
    FROM information_schema.columns 
    WHERE table_schema = CURRENT_SCHEMA() 
    AND table_name = 'products';
    
    IF products_id_type IS NULL THEN
        RAISE EXCEPTION 'Products table or id column not found in current schema: %', CURRENT_SCHEMA();
    END IF;
    
    RAISE NOTICE 'Current products.id type: %, columns: %', products_id_type, products_column_count;
    
    IF products_id_type = 'uuid' THEN
        RAISE NOTICE 'Products table already uses UUID primary key - checking for consistency';
    ELSIF products_id_type = 'bigint' THEN
        RAISE NOTICE 'Products table still uses BIGINT - migration needed';
    ELSE
        RAISE NOTICE 'Products table uses unexpected type: % - proceeding with caution', products_id_type;
    END IF;
END $$;

-- Step 3: Discover and drop ALL foreign key constraints that reference products.id
DO $$
DECLARE
    constraint_record RECORD;
    dropped_constraints TEXT[] := ARRAY[]::TEXT[];
BEGIN
    RAISE NOTICE 'Discovering foreign key constraints that reference products.id...';
    
    -- Find all foreign key constraints that reference products table
    FOR constraint_record IN
        SELECT 
            tc.constraint_name,
            tc.table_schema,
            tc.table_name,
            kcu.column_name,
            ccu.table_schema AS foreign_table_schema,
            ccu.table_name AS foreign_table_name,
            ccu.column_name AS foreign_column_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu 
            ON tc.constraint_name = kcu.constraint_name
            AND tc.table_schema = kcu.table_schema
        JOIN information_schema.constraint_column_usage ccu 
            ON ccu.constraint_name = tc.constraint_name
            AND ccu.table_schema = tc.table_schema
        WHERE tc.constraint_type = 'FOREIGN KEY'
        AND ccu.table_name = 'products'
        AND ccu.column_name = 'id'
        AND tc.table_schema = CURRENT_SCHEMA()
    LOOP
        -- Drop the foreign key constraint
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I', 
                      constraint_record.table_schema, 
                      constraint_record.table_name, 
                      constraint_record.constraint_name);
        
        dropped_constraints := dropped_constraints || 
            format('%s.%s.%s', constraint_record.table_schema, constraint_record.table_name, constraint_record.constraint_name);
        
        RAISE NOTICE 'Dropped FK constraint: % from table: %.%', 
            constraint_record.constraint_name, 
            constraint_record.table_schema,
            constraint_record.table_name;
    END LOOP;
    
    RAISE NOTICE 'Dropped % foreign key constraints: %', array_length(dropped_constraints, 1), dropped_constraints;
END $$;

-- Step 4: Add UUID columns if not exists and populate them
DO $$
DECLARE
    products_id_type text;
BEGIN
    SELECT data_type INTO products_id_type 
    FROM information_schema.columns 
    WHERE table_schema = CURRENT_SCHEMA() 
    AND table_name = 'products' 
    AND column_name = 'id';
    
    -- Only proceed if products table still has bigint id
    IF products_id_type = 'bigint' THEN
        -- Add new UUID column to products if not exists
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_schema = CURRENT_SCHEMA() 
                      AND table_name = 'products' 
                      AND column_name = 'new_id') THEN
            ALTER TABLE products ADD COLUMN new_id UUID DEFAULT uuid_generate_v4();
            RAISE NOTICE 'Added new_id UUID column to products table';
        END IF;
        
        -- Populate UUID values for existing records
        UPDATE products SET new_id = uuid_generate_v4() WHERE new_id IS NULL;
        RAISE NOTICE 'Populated UUID values for existing products';
        
        -- Add UUID columns to referencing tables
        -- Sale Items
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_schema = CURRENT_SCHEMA() 
                      AND table_name = 'sale_items' 
                      AND column_name = 'product_uuid') THEN
            ALTER TABLE sale_items ADD COLUMN product_uuid UUID;
            RAISE NOTICE 'Added product_uuid column to sale_items';
        END IF;
        
        -- Update sale_items foreign key mappings
        UPDATE sale_items 
        SET product_uuid = products.new_id 
        FROM products 
        WHERE sale_items.product_id = products.id;
        RAISE NOTICE 'Updated product references in sale_items';
        
        -- Handle discount_audit_log if it exists
        IF EXISTS (SELECT 1 FROM information_schema.tables 
                  WHERE table_schema = CURRENT_SCHEMA() 
                  AND table_name = 'discount_audit_log') THEN
            
            IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                          WHERE table_schema = CURRENT_SCHEMA() 
                          AND table_name = 'discount_audit_log' 
                          AND column_name = 'product_uuid') THEN
                ALTER TABLE discount_audit_log ADD COLUMN product_uuid UUID;
                RAISE NOTICE 'Added product_uuid column to discount_audit_log';
            END IF;
            
            -- Update discount_audit_log foreign key mappings
            UPDATE discount_audit_log 
            SET product_uuid = products.new_id 
            FROM products 
            WHERE discount_audit_log.product_id = products.id;
            RAISE NOTICE 'Updated product references in discount_audit_log';
        END IF;
        
    ELSE
        RAISE NOTICE 'Products table already uses UUID, skipping column addition';
    END IF;
END $$;

-- Step 5: Migrate primary key from bigint to UUID
DO $$
DECLARE
    products_id_type text;
BEGIN
    SELECT data_type INTO products_id_type 
    FROM information_schema.columns 
    WHERE table_schema = CURRENT_SCHEMA() 
    AND table_name = 'products' 
    AND column_name = 'id';
    
    IF products_id_type = 'bigint' THEN
        -- Drop primary key constraint
        ALTER TABLE products DROP CONSTRAINT IF EXISTS products_pkey;
        RAISE NOTICE 'Dropped products primary key constraint';
        
        -- Drop the old bigint id column
        ALTER TABLE products DROP COLUMN id;
        RAISE NOTICE 'Dropped old bigint id column';
        
        -- Rename new_id to id
        ALTER TABLE products RENAME COLUMN new_id TO id;
        RAISE NOTICE 'Renamed new_id to id';
        
        -- Add primary key constraint on UUID column
        ALTER TABLE products ADD PRIMARY KEY (id);
        RAISE NOTICE 'Added new UUID primary key constraint';
        
    ELSE
        -- Clean up temporary column if exists
        IF EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_schema = CURRENT_SCHEMA() 
                  AND table_name = 'products' 
                  AND column_name = 'new_id') THEN
            ALTER TABLE products DROP COLUMN new_id;
            RAISE NOTICE 'Cleaned up temporary new_id column';
        END IF;
    END IF;
END $$;

-- Step 6: Update foreign key columns to use UUID type
DO $$
BEGIN
    -- Sale Items: Replace bigint product_id with UUID
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'sale_items' 
              AND column_name = 'product_uuid') THEN
        
        -- Drop old product_id column
        ALTER TABLE sale_items DROP COLUMN IF EXISTS product_id;
        
        -- Rename product_uuid to product_id
        ALTER TABLE sale_items RENAME COLUMN product_uuid TO product_id;
        
        -- Set NOT NULL constraint
        ALTER TABLE sale_items ALTER COLUMN product_id SET NOT NULL;
        
        RAISE NOTICE 'Updated sale_items.product_id to UUID type';
    END IF;
    
    -- Discount Audit Log: Replace bigint product_id with UUID
    IF EXISTS (SELECT 1 FROM information_schema.tables 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'discount_audit_log') THEN
        
        IF EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_schema = CURRENT_SCHEMA() 
                  AND table_name = 'discount_audit_log' 
                  AND column_name = 'product_uuid') THEN
            
            -- Drop old product_id column
            ALTER TABLE discount_audit_log DROP COLUMN IF EXISTS product_id;
            
            -- Rename product_uuid to product_id
            ALTER TABLE discount_audit_log RENAME COLUMN product_uuid TO product_id;
            
            -- Set NOT NULL constraint
            ALTER TABLE discount_audit_log ALTER COLUMN product_id SET NOT NULL;
            
            RAISE NOTICE 'Updated discount_audit_log.product_id to UUID type';
        END IF;
    END IF;
END $$;

-- Step 7: Recreate foreign key constraints with proper names
DO $$
BEGIN
    -- Recreate foreign key for sale_items -> products
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                  WHERE constraint_name = 'fk_sale_items_product_id' 
                  AND table_name = 'sale_items'
                  AND table_schema = CURRENT_SCHEMA()) THEN
        
        ALTER TABLE sale_items 
        ADD CONSTRAINT fk_sale_items_product_id 
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
        
        RAISE NOTICE 'Added foreign key constraint: fk_sale_items_product_id';
    END IF;
    
    -- Recreate foreign key for discount_audit_log -> products
    IF EXISTS (SELECT 1 FROM information_schema.tables 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'discount_audit_log') THEN
        
        IF EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_schema = CURRENT_SCHEMA() 
                  AND table_name = 'discount_audit_log' 
                  AND column_name = 'product_id') THEN
            
            IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                          WHERE constraint_name = 'fk_discount_audit_log_product_id' 
                          AND table_name = 'discount_audit_log'
                          AND table_schema = CURRENT_SCHEMA()) THEN
                
                ALTER TABLE discount_audit_log 
                ADD CONSTRAINT fk_discount_audit_log_product_id 
                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;
                
                RAISE NOTICE 'Added foreign key constraint: fk_discount_audit_log_product_id';
            END IF;
        END IF;
    END IF;
END $$;

-- Step 8: Create performance indexes
DO $$
BEGIN
    -- Index on products.id (primary key)
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                  WHERE schemaname = CURRENT_SCHEMA() 
                  AND tablename = 'products' 
                  AND indexname = 'idx_products_id_uuid') THEN
        CREATE INDEX idx_products_id_uuid ON products(id);
        RAISE NOTICE 'Created index: idx_products_id_uuid';
    END IF;
    
    -- Index on sale_items.product_id
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                  WHERE schemaname = CURRENT_SCHEMA() 
                  AND tablename = 'sale_items' 
                  AND indexname = 'idx_sale_items_product_id_uuid') THEN
        CREATE INDEX idx_sale_items_product_id_uuid ON sale_items(product_id);
        RAISE NOTICE 'Created index: idx_sale_items_product_id_uuid';
    END IF;
    
    -- Index on discount_audit_log.product_id (if table exists)
    IF EXISTS (SELECT 1 FROM information_schema.tables 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'discount_audit_log') THEN
        
        IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                      WHERE schemaname = CURRENT_SCHEMA() 
                      AND tablename = 'discount_audit_log' 
                      AND indexname = 'idx_discount_audit_log_product_id_uuid') THEN
            CREATE INDEX idx_discount_audit_log_product_id_uuid ON discount_audit_log(product_id);
            RAISE NOTICE 'Created index: idx_discount_audit_log_product_id_uuid';
        END IF;
    END IF;
END $$;

-- Step 9: Remove any redundant UUID columns (cleanup from previous migrations)
DO $$
BEGIN
    -- Remove old uuid column from products if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'products' 
              AND column_name = 'uuid') THEN
        ALTER TABLE products DROP COLUMN uuid;
        RAISE NOTICE 'Removed redundant uuid column from products';
    END IF;
    
    -- Remove any leftover UUID mapping columns
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'sale_items' 
              AND column_name = 'product_uuid') THEN
        ALTER TABLE sale_items DROP COLUMN product_uuid;
        RAISE NOTICE 'Cleaned up product_uuid column from sale_items';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'discount_audit_log' 
              AND column_name = 'product_uuid') THEN
        ALTER TABLE discount_audit_log DROP COLUMN product_uuid;
        RAISE NOTICE 'Cleaned up product_uuid column from discount_audit_log';
    END IF;
END $$;

-- Step 10: Final validation
DO $$
DECLARE
    products_id_type text;
    sale_items_product_id_type text;
    discount_audit_log_product_id_type text;
    products_count integer;
    sale_items_count integer;
    discount_audit_log_count integer;
    fk_constraints_count integer;
BEGIN
    -- Get final column types
    SELECT data_type INTO products_id_type 
    FROM information_schema.columns 
    WHERE table_schema = CURRENT_SCHEMA() 
    AND table_name = 'products' 
    AND column_name = 'id';
    
    SELECT data_type INTO sale_items_product_id_type 
    FROM information_schema.columns 
    WHERE table_schema = CURRENT_SCHEMA() 
    AND table_name = 'sale_items' 
    AND column_name = 'product_id';
    
    IF EXISTS (SELECT 1 FROM information_schema.tables 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'discount_audit_log') THEN
        SELECT data_type INTO discount_audit_log_product_id_type 
        FROM information_schema.columns 
        WHERE table_schema = CURRENT_SCHEMA() 
        AND table_name = 'discount_audit_log' 
        AND column_name = 'product_id';
    END IF;
    
    -- Get record counts
    SELECT COUNT(*) INTO products_count FROM products;
    SELECT COUNT(*) INTO sale_items_count FROM sale_items;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'discount_audit_log') THEN
        SELECT COUNT(*) INTO discount_audit_log_count FROM discount_audit_log;
    ELSE
        discount_audit_log_count := 0;
    END IF;
    
    -- Count foreign key constraints
    SELECT COUNT(*) INTO fk_constraints_count
    FROM information_schema.table_constraints 
    WHERE constraint_type = 'FOREIGN KEY' 
    AND table_schema = CURRENT_SCHEMA()
    AND (table_name = 'sale_items' OR table_name = 'discount_audit_log');
    
    -- Validation report
    RAISE NOTICE 'MIGRATION VALIDATION REPORT:';
    RAISE NOTICE '========================';
    RAISE NOTICE 'products.id type: %', products_id_type;
    RAISE NOTICE 'sale_items.product_id type: %', sale_items_product_id_type;
    RAISE NOTICE 'discount_audit_log.product_id type: %', COALESCE(discount_audit_log_product_id_type, 'N/A');
    RAISE NOTICE 'Records - products: %, sale_items: %, discount_audit_log: %', 
                 products_count, sale_items_count, discount_audit_log_count;
    RAISE NOTICE 'Foreign key constraints: %', fk_constraints_count;
    
    -- Verify UUID format in products
    IF EXISTS (SELECT 1 FROM products 
              WHERE id::text !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$') THEN
        RAISE EXCEPTION 'Invalid UUID format found in products.id column';
    END IF;
    
    -- Verify all columns are UUID type
    IF products_id_type != 'uuid' THEN
        RAISE EXCEPTION 'products.id is not UUID type: %', products_id_type;
    END IF;
    
    IF sale_items_product_id_type != 'uuid' THEN
        RAISE EXCEPTION 'sale_items.product_id is not UUID type: %', sale_items_product_id_type;
    END IF;
    
    IF discount_audit_log_product_id_type IS NOT NULL AND discount_audit_log_product_id_type != 'uuid' THEN
        RAISE EXCEPTION 'discount_audit_log.product_id is not UUID type: %', discount_audit_log_product_id_type;
    END IF;
    
    RAISE NOTICE 'All validations passed - UUID migration completed successfully!';
END $$;

-- Step 11: Log completion in events table
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables 
              WHERE table_schema = CURRENT_SCHEMA() 
              AND table_name = 'events') THEN
        INSERT INTO events (name, description, event_type, priority, start_time, created_at, created_by)
        VALUES (
            'UUID Products Migration Completed',
            'Successfully migrated products table from bigint to UUID primary keys. All foreign key references in sale_items and discount_audit_log have been updated. Migration handles constraint discovery dynamically.',
            'SYSTEM',
            'HIGH',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP,
            'SYSTEM_UUID_MIGRATION'
        ) ON CONFLICT DO NOTHING;
        
        RAISE NOTICE 'Logged migration completion in events table';
    END IF;
END $$;

-- Final notice
DO $$
BEGIN
    RAISE NOTICE 'UUID migration completed successfully at %', CURRENT_TIMESTAMP;
    RAISE NOTICE 'Backup tables created: products_backup_uuid_final, sale_items_backup_uuid_final, discount_audit_log_backup_uuid_final';
    RAISE NOTICE 'To clean up backup tables after validation, run: DROP TABLE IF EXISTS products_backup_uuid_final, sale_items_backup_uuid_final, discount_audit_log_backup_uuid_final;';
END $$;

-- Commit transaction
COMMIT;