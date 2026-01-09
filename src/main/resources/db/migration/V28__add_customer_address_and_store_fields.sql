-- Add address fields and store reference to customers table

-- Add address fields
ALTER TABLE customers ADD COLUMN IF NOT EXISTS address VARCHAR(200);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS state VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS country VARCHAR(100);

-- Add optional store reference
ALTER TABLE customers ADD COLUMN IF NOT EXISTS store_id UUID REFERENCES stores(id) ON DELETE SET NULL;

-- Add index for store_id for better query performance
CREATE INDEX IF NOT EXISTS idx_customers_store ON customers(store_id);
