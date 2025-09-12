-- UUID Support Migration for InventSight
-- Generated: 2025-08-26
-- Description: Adds UUID fields to users and products tables and creates tenant_id field for users

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Add UUID columns to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS uuid VARCHAR(36) UNIQUE,
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(36);

-- Add UUID column to products table  
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS uuid VARCHAR(36) UNIQUE;

-- Function to generate UUIDs for existing users
DO $$
DECLARE
    user_record RECORD;
    new_uuid VARCHAR(36);
BEGIN
    -- Update existing users that don't have UUIDs
    FOR user_record IN SELECT id FROM users WHERE uuid IS NULL OR uuid = ''
    LOOP
        new_uuid := uuid_generate_v4()::text;
        UPDATE users 
        SET uuid = new_uuid, 
            tenant_id = new_uuid,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = user_record.id;
        
        RAISE NOTICE 'Updated user ID % with UUID %', user_record.id, new_uuid;
    END LOOP;
END $$;

-- Function to generate UUIDs for existing products
DO $$
DECLARE
    product_record RECORD;
    new_uuid VARCHAR(36);
BEGIN
    -- Update existing products that don't have UUIDs
    FOR product_record IN SELECT id FROM products WHERE uuid IS NULL OR uuid = ''
    LOOP
        new_uuid := uuid_generate_v4()::text;
        UPDATE products 
        SET uuid = new_uuid,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = product_record.id;
        
        RAISE NOTICE 'Updated product ID % with UUID %', product_record.id, new_uuid;
    END LOOP;
END $$;

-- Make UUID columns NOT NULL after populating existing data
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE products ALTER COLUMN uuid SET NOT NULL;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_uuid ON users(uuid);
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_uuid ON products(uuid);

-- Create a view to show user-tenant mapping
CREATE OR REPLACE VIEW user_tenant_mapping AS
SELECT 
    id,
    username,
    email,
    uuid,
    tenant_id,
    first_name,
    last_name,
    created_at
FROM users
WHERE is_active = true;

COMMENT ON VIEW user_tenant_mapping IS 'Maps users to their tenant IDs (UUIDs) for schema-based multi-tenancy';

-- Log migration completion
INSERT INTO events (name, description, event_type, priority, start_time, created_at, created_by)
VALUES (
    'UUID Migration Completed',
    'Successfully added UUID support to users and products tables. All existing records have been assigned UUIDs.',
    'SYSTEM',
    'HIGH',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'SYSTEM_MIGRATION'
) ON CONFLICT DO NOTHING;

-- Print completion message
DO $$
BEGIN
    RAISE NOTICE 'UUID migration completed successfully!';
    RAISE NOTICE 'Users now have uuid and tenant_id fields';
    RAISE NOTICE 'Products now have uuid fields';
    RAISE NOTICE 'All existing records have been assigned UUIDs';
END $$;