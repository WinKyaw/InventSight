-- Create customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT DEFAULT 0,
    
    -- Customer Details
    name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    customer_type VARCHAR(20) NOT NULL DEFAULT 'GUEST',
    notes TEXT,
    discount_percentage DECIMAL(5, 2) DEFAULT 0,
    total_purchases DECIMAL(12, 2) DEFAULT 0,
    
    -- Multi-tenant Fields
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID REFERENCES users(id)
);

-- Indexes for customers
CREATE INDEX idx_customers_company ON customers(company_id);
CREATE INDEX idx_customers_phone ON customers(phone_number);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_active ON customers(is_active);
CREATE INDEX idx_customers_type ON customers(customer_type);
CREATE INDEX idx_customers_name ON customers(name);

-- Add predefined_item_sku column to products table
ALTER TABLE products ADD COLUMN IF NOT EXISTS predefined_item_sku VARCHAR(11);

-- Add index for predefined_item_sku
CREATE INDEX IF NOT EXISTS idx_products_predefined_item_sku ON products(predefined_item_sku);

-- Add index for SKU uniqueness in predefined_items
CREATE INDEX IF NOT EXISTS idx_predefined_items_sku ON predefined_items(sku);
