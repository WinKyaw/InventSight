-- Migration V22: Remove redundant user_store_roles table
-- This table is fully redundant with company_store_user_roles
-- All functionality has been migrated to company_store_user_roles

-- Drop the user_store_roles table
DROP TABLE IF EXISTS user_store_roles CASCADE;

-- Add comment explaining the removal
-- Note: This is logged in migration history automatically by Flyway
