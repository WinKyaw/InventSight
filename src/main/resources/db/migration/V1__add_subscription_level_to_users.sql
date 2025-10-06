-- Flyway migration to add subscription_level column to users table
-- This ensures the column exists with a default value of 'FREE'
-- Compatible with both PostgreSQL and H2

-- Add subscription_level column (if it doesn't exist, this will work; if it does, it will fail gracefully)
-- Using ALTER TABLE with column addition
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_level VARCHAR(20) DEFAULT 'FREE';

-- Update existing rows to have FREE subscription if null (safeguard)
UPDATE users SET subscription_level = 'FREE' WHERE subscription_level IS NULL;
