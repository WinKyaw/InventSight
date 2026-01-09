-- V30__enhance_sales_table.sql
-- Add customer relationship, company relationship, fulfillment tracking, and delivery tracking to sales table

-- Add customer relationship (replacing plain text fields)
ALTER TABLE sales 
ADD COLUMN customer_id UUID,
ADD CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES customers(id);

-- Add company relationship
-- Note: Make it nullable initially to avoid breaking existing data
ALTER TABLE sales 
ADD COLUMN company_id UUID,
ADD CONSTRAINT fk_sale_company FOREIGN KEY (company_id) REFERENCES companies(id);

-- Update existing sales to get company_id from their store
UPDATE sales 
SET company_id = stores.company_id
FROM stores
WHERE sales.store_id = stores.id
AND sales.company_id IS NULL;

-- Now make company_id NOT NULL
ALTER TABLE sales 
ALTER COLUMN company_id SET NOT NULL;

-- Add fulfilled_by employee
ALTER TABLE sales 
ADD COLUMN fulfilled_by_user_id UUID,
ADD COLUMN fulfilled_at TIMESTAMP,
ADD CONSTRAINT fk_sale_fulfilled_by FOREIGN KEY (fulfilled_by_user_id) REFERENCES users(id);

-- Add delivery person (optional)
ALTER TABLE sales 
ADD COLUMN delivery_person_id UUID,
ADD COLUMN delivery_assigned_at TIMESTAMP,
ADD COLUMN delivered_at TIMESTAMP,
ADD COLUMN delivery_notes TEXT,
ADD CONSTRAINT fk_sale_delivery_person FOREIGN KEY (delivery_person_id) REFERENCES users(id);

-- Add receipt type
ALTER TABLE sales 
ADD COLUMN receipt_type VARCHAR(20) DEFAULT 'IN_STORE';

-- Add indexes
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_company ON sales(company_id);
CREATE INDEX idx_sales_fulfilled_by ON sales(fulfilled_by_user_id);
CREATE INDEX idx_sales_delivery_person ON sales(delivery_person_id);
CREATE INDEX idx_sales_receipt_type ON sales(receipt_type);

-- Keep old customer text fields for backwards compatibility
-- customerName, customerEmail, customerPhone will remain for legacy support
