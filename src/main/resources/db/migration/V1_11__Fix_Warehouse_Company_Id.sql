-- Fix existing warehouses with NULL company_id
-- This assigns them to the first active company (assuming single-company setup during development)

-- Get the first active company ID and update warehouses
DO $$
DECLARE
    first_company_id UUID;
    updated_count INTEGER;
BEGIN
    -- Get first active company
    SELECT id INTO first_company_id 
    FROM companies 
    WHERE is_active = true 
    ORDER BY created_at ASC 
    LIMIT 1;
    
    -- Update warehouses with NULL company_id
    IF first_company_id IS NOT NULL THEN
        UPDATE warehouses 
        SET company_id = first_company_id,
            updated_at = CURRENT_TIMESTAMP
        WHERE company_id IS NULL;
        
        GET DIAGNOSTICS updated_count = ROW_COUNT;
        
        RAISE NOTICE 'Updated % warehouses with company_id: %', 
            updated_count,
            first_company_id;
    ELSE
        RAISE NOTICE 'No active companies found - warehouses not updated';
    END IF;
END $$;

-- Add NOT NULL constraint to prevent future NULL values
ALTER TABLE warehouses 
ALTER COLUMN company_id SET NOT NULL;

-- Add foreign key constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_warehouse_company' 
        AND table_name = 'warehouses'
    ) THEN
        ALTER TABLE warehouses
        ADD CONSTRAINT fk_warehouse_company
        FOREIGN KEY (company_id) 
        REFERENCES companies(id) 
        ON DELETE RESTRICT;
    END IF;
END $$;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_warehouses_company_id 
ON warehouses(company_id) 
WHERE is_active = true;
