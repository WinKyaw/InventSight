-- Flyway migration to add subscription_level column to users table
-- This ensures the column exists with a default value of 'FREE'

-- Add subscription_level column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'subscription_level'
    ) THEN
        ALTER TABLE users
        ADD COLUMN subscription_level VARCHAR(20) DEFAULT 'FREE';
        
        -- Update existing rows to have FREE subscription if null
        UPDATE users
        SET subscription_level = 'FREE'
        WHERE subscription_level IS NULL;
        
        RAISE NOTICE 'Added subscription_level column to users table with default value FREE';
    ELSE
        RAISE NOTICE 'subscription_level column already exists in users table';
    END IF;
END $$;
