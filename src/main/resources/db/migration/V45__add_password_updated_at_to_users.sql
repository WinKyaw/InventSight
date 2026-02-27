-- Track when each user last changed their password
-- Supports future password expiry policies
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_updated_at TIMESTAMP;
UPDATE users SET password_updated_at = updated_at WHERE password_updated_at IS NULL;
