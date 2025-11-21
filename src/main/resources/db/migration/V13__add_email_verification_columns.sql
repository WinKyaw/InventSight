-- V13__add_email_verification_columns.sql
-- Add email verification fields to users table

-- Add columns for email verification
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token_expires_at TIMESTAMP;

-- Create index for verification token lookup
CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(verification_token);

-- Add comments for documentation
COMMENT ON COLUMN users.is_email_verified IS 'Whether the user email has been verified';
COMMENT ON COLUMN users.verification_token IS 'Token for email verification';
COMMENT ON COLUMN users.verification_token_expires_at IS 'Expiration time for verification token (24 hours)';

-- Update existing users to have verified emails (backward compatibility)
-- Check for email_verified column first
UPDATE users 
SET is_email_verified = COALESCE(email_verified, TRUE) 
WHERE is_email_verified IS NULL;
