-- Add total_sales column if it doesn't exist
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS total_sales INTEGER DEFAULT 0;

-- Add last_sold_date column if it doesn't exist
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS last_sold_date TIMESTAMP;

-- Update existing products to have 0 sales
UPDATE products 
SET total_sales = 0 
WHERE total_sales IS NULL;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_products_total_sales ON products(total_sales);
CREATE INDEX IF NOT EXISTS idx_products_last_sold_date ON products(last_sold_date);
