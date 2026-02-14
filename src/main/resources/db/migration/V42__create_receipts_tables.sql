-- V42__create_receipts_tables.sql
-- Create receipts system: separate receipts table with junction tables for sales and transfers
-- This normalizes the schema and allows receipts to be reused across different transaction types

-- Create receipts table (central receipt data)
CREATE TABLE receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_number VARCHAR(50) UNIQUE NOT NULL,
    receipt_type VARCHAR(20) NOT NULL,  -- SALE, TRANSFER, RETURN, etc.
    
    -- Status tracking
    status VARCHAR(20) NOT NULL,        -- PENDING, COMPLETED, CANCELLED
    
    -- Payment information
    payment_method VARCHAR(50),         -- CASH, CREDIT_CARD, DEBIT_CARD, etc.
    payment_status VARCHAR(20),         -- UNPAID, PAID, PARTIALLY_PAID, REFUNDED
    paid_amount DECIMAL(15, 2),
    payment_date TIMESTAMP,
    
    -- Financial summary
    subtotal DECIMAL(15, 2) NOT NULL,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) NOT NULL,
    
    -- Fulfillment tracking
    fulfilled_by_user_id UUID,
    fulfilled_at TIMESTAMP,
    
    -- Delivery tracking (for delivery receipts)
    delivery_type VARCHAR(20),          -- IN_STORE, DELIVERY, PICKUP
    delivery_person_id UUID,
    delivery_assigned_at TIMESTAMP,
    delivered_at TIMESTAMP,
    delivery_notes TEXT,
    
    -- Customer information
    customer_id UUID,
    customer_name VARCHAR(200),
    customer_email VARCHAR(100),
    customer_phone VARCHAR(20),
    
    -- Multi-tenancy
    company_id UUID NOT NULL,
    store_id UUID,
    
    -- Audit fields
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Foreign keys
    CONSTRAINT fk_receipt_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_receipt_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_receipt_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_receipt_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_receipt_fulfilled_by FOREIGN KEY (fulfilled_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_receipt_delivery_person FOREIGN KEY (delivery_person_id) REFERENCES users(id)
);

-- Create sale_receipts junction table
CREATE TABLE sale_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL,
    sale_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sale_receipt_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_receipt_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT uk_sale_receipt UNIQUE (receipt_id, sale_id)
);

-- Create transfer_receipts junction table
CREATE TABLE transfer_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL,
    transfer_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transfer_receipt_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_receipt_transfer FOREIGN KEY (transfer_id) REFERENCES transfer_requests(id) ON DELETE CASCADE,
    CONSTRAINT uk_transfer_receipt UNIQUE (receipt_id, transfer_id)
);

-- Create receipt_items table (receipt line items)
CREATE TABLE receipt_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(200),      -- Denormalized for history
    product_sku VARCHAR(100),       -- Denormalized for history
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    total_price DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_receipt_item_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Indexes for receipts table
CREATE INDEX idx_receipts_receipt_number ON receipts(receipt_number);
CREATE INDEX idx_receipts_status ON receipts(status);
CREATE INDEX idx_receipts_payment_status ON receipts(payment_status);
CREATE INDEX idx_receipts_company ON receipts(company_id);
CREATE INDEX idx_receipts_store ON receipts(store_id);
CREATE INDEX idx_receipts_customer ON receipts(customer_id);
CREATE INDEX idx_receipts_created_at ON receipts(created_at DESC);
CREATE INDEX idx_receipts_type_status ON receipts(receipt_type, status);

-- Indexes for sale_receipts table
CREATE INDEX idx_sale_receipts_receipt ON sale_receipts(receipt_id);
CREATE INDEX idx_sale_receipts_sale ON sale_receipts(sale_id);

-- Indexes for transfer_receipts table
CREATE INDEX idx_transfer_receipts_receipt ON transfer_receipts(receipt_id);
CREATE INDEX idx_transfer_receipts_transfer ON transfer_receipts(transfer_id);

-- Indexes for receipt_items table
CREATE INDEX idx_receipt_items_receipt ON receipt_items(receipt_id);
CREATE INDEX idx_receipt_items_product ON receipt_items(product_id);
