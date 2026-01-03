-- Add company_id to products table
ALTER TABLE products ADD COLUMN company_id UUID;

-- Add warehouse_id to products table (for warehouse inventory tracking)
ALTER TABLE products ADD COLUMN warehouse_id UUID;

-- Add low_stock_threshold to products table
ALTER TABLE products ADD COLUMN low_stock_threshold INTEGER NOT NULL DEFAULT 5;

-- Add predefined_item_id to products table (link back to master catalog)
ALTER TABLE products ADD COLUMN predefined_item_id UUID;

-- Make default_price MANDATORY in predefined_items
-- Update existing NULL values first
UPDATE predefined_items SET default_price = 0.00 WHERE default_price IS NULL;

-- Then add NOT NULL constraint
ALTER TABLE predefined_items ALTER COLUMN default_price SET NOT NULL;

-- Add foreign key constraints
ALTER TABLE products ADD CONSTRAINT fk_products_company 
    FOREIGN KEY (company_id) REFERENCES companies(id);

ALTER TABLE products ADD CONSTRAINT fk_products_warehouse 
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE products ADD CONSTRAINT fk_products_predefined_item 
    FOREIGN KEY (predefined_item_id) REFERENCES predefined_items(id);

-- Create indexes for performance
CREATE INDEX idx_products_company_id ON products(company_id);
CREATE INDEX idx_products_warehouse_id ON products(warehouse_id);
CREATE INDEX idx_products_predefined_item_id ON products(predefined_item_id);

-- Make store_id nullable (products can be in either store or warehouse)
ALTER TABLE products ALTER COLUMN store_id DROP NOT NULL;

-- Backfill company_id for existing products based on their store's company
UPDATE products 
SET company_id = stores.company_id
FROM stores
WHERE products.store_id = stores.id
AND products.company_id IS NULL;

-- Now make company_id NOT NULL
ALTER TABLE products ALTER COLUMN company_id SET NOT NULL;
