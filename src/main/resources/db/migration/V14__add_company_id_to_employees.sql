-- Migration V14: Add company_id to employees table for proper company association
-- This ensures employees are associated with both company and store

-- Add company_id column to employees table if it doesn't exist
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS company_id UUID;

-- Add foreign key constraint linking employees to companies
ALTER TABLE IF EXISTS employees 
    ADD CONSTRAINT fk_employees_company 
    FOREIGN KEY (company_id) 
    REFERENCES companies(id)
    ON DELETE RESTRICT;

-- Create index for performance on company_id lookups
CREATE INDEX IF NOT EXISTS idx_employees_company_id ON employees(company_id);

-- Update existing employees to link them to the company that owns their store
-- This backfills the company_id for existing employees
UPDATE employees e
SET company_id = (
    SELECT s.company_id
    FROM stores s
    WHERE s.id = e.store_id
)
WHERE e.company_id IS NULL
  AND e.store_id IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM stores s WHERE s.id = e.store_id AND s.company_id IS NOT NULL
  );

-- Make company_id NOT NULL after backfilling existing data
ALTER TABLE IF EXISTS employees ALTER COLUMN company_id SET NOT NULL;
