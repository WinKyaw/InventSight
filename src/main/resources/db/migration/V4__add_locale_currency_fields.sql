-- Flyway migration to add internationalization and multi-currency support
-- PostgreSQL-compatible with IF NOT EXISTS checks

-- Add locale and currency fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS locale VARCHAR(10) DEFAULT 'en';
ALTER TABLE users ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3) DEFAULT 'USD';

-- Add locale and currency defaults to companies table
ALTER TABLE companies ADD COLUMN IF NOT EXISTS default_locale VARCHAR(10) DEFAULT 'en';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS default_currency VARCHAR(3) DEFAULT 'USD';

-- Create index for faster lookup
CREATE INDEX IF NOT EXISTS idx_users_locale ON users(locale);
CREATE INDEX IF NOT EXISTS idx_users_currency ON users(currency_code);
CREATE INDEX IF NOT EXISTS idx_companies_locale ON companies(default_locale);
CREATE INDEX IF NOT EXISTS idx_companies_currency ON companies(default_currency);
