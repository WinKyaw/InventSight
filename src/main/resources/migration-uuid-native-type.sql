-- Migration to convert varchar UUID columns to native PostgreSQL UUID type
-- Generated: 2025-09-12
-- Description: Updates users table uuid and tenant_id columns to use native PostgreSQL UUID type
-- This migration fixes the issue where Java UUID objects couldn't be inserted into varchar columns

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create a backup of important user data before the migration
CREATE TABLE IF NOT EXISTS users_uuid_migration_backup AS 
SELECT id, username, email, uuid, tenant_id, created_at
FROM users 
WHERE uuid IS NOT NULL;

-- Step 1: Add temporary columns with UUID type
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS uuid_new UUID,
ADD COLUMN IF NOT EXISTS tenant_id_new UUID;

-- Step 2: Convert existing varchar UUIDs to native UUID type
-- Handle potential conversion errors gracefully
DO $$
DECLARE
    user_record RECORD;
BEGIN
    FOR user_record IN SELECT id, uuid, tenant_id FROM users WHERE uuid IS NOT NULL
    LOOP
        BEGIN
            -- Convert varchar UUID to native UUID
            UPDATE users 
            SET uuid_new = user_record.uuid::UUID,
                tenant_id_new = COALESCE(user_record.tenant_id, user_record.uuid)::UUID
            WHERE id = user_record.id;
            
            RAISE NOTICE 'Converted UUID for user ID %: % -> UUID type', user_record.id, user_record.uuid;
        EXCEPTION
            WHEN OTHERS THEN
                -- If conversion fails, generate a new UUID
                RAISE WARNING 'Failed to convert UUID for user ID %, generating new UUID: %', user_record.id, SQLERRM;
                UPDATE users 
                SET uuid_new = uuid_generate_v4(),
                    tenant_id_new = uuid_generate_v4()
                WHERE id = user_record.id;
        END;
    END LOOP;
END $$;

-- Step 3: Handle users without UUIDs (assign new ones)
UPDATE users 
SET uuid_new = uuid_generate_v4(),
    tenant_id_new = uuid_new
WHERE uuid_new IS NULL;

-- Step 4: Drop old columns and rename new ones
ALTER TABLE users DROP COLUMN IF EXISTS uuid;
ALTER TABLE users DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE users RENAME COLUMN uuid_new TO uuid;
ALTER TABLE users RENAME COLUMN tenant_id_new TO tenant_id;

-- Step 5: Add constraints back
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_uuid_unique UNIQUE (uuid);

-- Step 6: Recreate indexes for performance
DROP INDEX IF EXISTS idx_users_uuid;
DROP INDEX IF EXISTS idx_users_tenant_id;
CREATE INDEX idx_users_uuid ON users(uuid);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);

-- Step 7: Update the user_tenant_mapping view
DROP VIEW IF EXISTS user_tenant_mapping;
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

COMMENT ON VIEW user_tenant_mapping IS 'Maps users to their tenant IDs (native PostgreSQL UUIDs) for schema-based multi-tenancy';

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE 'Native UUID migration completed successfully!';
    RAISE NOTICE 'Users table now uses native PostgreSQL UUID type for uuid and tenant_id columns';
    RAISE NOTICE 'This resolves JPA mapping issues between Java UUID objects and database columns';
END $$;