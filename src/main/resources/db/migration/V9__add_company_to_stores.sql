-- Migration V9: Add company_id to stores table for proper schema-based multi-tenancy
-- This fixes cross-schema foreign key constraint issues between products and stores

-- Add company_id column to stores table if it doesn't exist
ALTER TABLE IF EXISTS stores ADD COLUMN IF NOT EXISTS company_id UUID;

-- Add foreign key constraint linking stores to companies
ALTER TABLE IF EXISTS stores 
    ADD CONSTRAINT fk_stores_company 
    FOREIGN KEY (company_id) 
    REFERENCES companies(id)
    ON DELETE RESTRICT;

-- Create index for performance on company_id lookups
CREATE INDEX IF NOT EXISTS idx_stores_company_id ON stores(company_id);

-- Update existing stores to link them to companies based on the user who created them
-- This backfills the company_id for existing stores
UPDATE stores s
SET company_id = (
    SELECT c.id
    FROM companies c
    WHERE c.created_by = s.created_by
    ORDER BY c.created_at ASC
    LIMIT 1
)
WHERE s.company_id IS NULL
  AND EXISTS (
    SELECT 1 FROM companies c WHERE c.created_by = s.created_by
  );
