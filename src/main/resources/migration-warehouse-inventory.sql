-- Warehouse Inventory Management System Migration
-- Generated: 2025-01-27
-- Description: Creates tables for comprehensive warehouse inventory management

-- Enable UUID extension if not already enabled (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Start transaction
BEGIN;

-- 1. Warehouses table
CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    location VARCHAR(500) NOT NULL,
    address VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    manager_name VARCHAR(100),
    warehouse_type VARCHAR(50) DEFAULT 'GENERAL', -- GENERAL, COLD_STORAGE, HAZMAT, etc.
    capacity_cubic_meters DECIMAL(12,2),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- 2. Warehouse Inventory table (tracks current stock levels per warehouse per product)
CREATE TABLE IF NOT EXISTS warehouse_inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    current_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0, -- Reserved for orders/allocations
    available_quantity INTEGER GENERATED ALWAYS AS (current_quantity - reserved_quantity) STORED,
    minimum_stock_level INTEGER DEFAULT 0,
    maximum_stock_level INTEGER,
    reorder_point INTEGER DEFAULT 0,
    location_in_warehouse VARCHAR(100), -- e.g., "Aisle A, Shelf 3, Bin 5"
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT fk_warehouse_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_inventory_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_warehouse_product UNIQUE (warehouse_id, product_id),
    CONSTRAINT chk_quantities CHECK (current_quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= current_quantity)
);

-- 3. Warehouse Inventory Additions table (tracks all inventory additions/receipts)
CREATE TABLE IF NOT EXISTS warehouse_inventory_additions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(12,2),
    supplier_name VARCHAR(200),
    reference_number VARCHAR(100), -- PO number, invoice number, etc.
    receipt_date DATE DEFAULT CURRENT_DATE,
    expiry_date DATE,
    batch_number VARCHAR(100),
    notes VARCHAR(500),
    transaction_type VARCHAR(50) DEFAULT 'RECEIPT', -- RECEIPT, TRANSFER_IN, ADJUSTMENT_IN, RETURN
    status VARCHAR(50) DEFAULT 'COMPLETED', -- PENDING, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT fk_warehouse_additions_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_additions_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_addition_quantity CHECK (quantity > 0)
);

-- 4. Warehouse Inventory Withdrawals table (tracks all inventory withdrawals/issues)
CREATE TABLE IF NOT EXISTS warehouse_inventory_withdrawals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_cost DECIMAL(10,2),
    total_cost DECIMAL(12,2),
    destination VARCHAR(200), -- Store, Customer, Department, etc.
    reference_number VARCHAR(100), -- Sale ID, Transfer order, etc.
    withdrawal_date DATE DEFAULT CURRENT_DATE,
    reason VARCHAR(100), -- SALE, TRANSFER_OUT, ADJUSTMENT_OUT, DAMAGE, EXPIRED
    notes VARCHAR(500),
    transaction_type VARCHAR(50) DEFAULT 'ISSUE', -- ISSUE, TRANSFER_OUT, ADJUSTMENT_OUT, DAMAGE
    status VARCHAR(50) DEFAULT 'COMPLETED', -- PENDING, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT fk_warehouse_withdrawals_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_withdrawals_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_withdrawal_quantity CHECK (quantity > 0)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_warehouses_active ON warehouses(is_active);
CREATE INDEX IF NOT EXISTS idx_warehouses_type ON warehouses(warehouse_type);
CREATE INDEX IF NOT EXISTS idx_warehouse_inventory_warehouse ON warehouse_inventory(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_inventory_product ON warehouse_inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_inventory_low_stock ON warehouse_inventory(warehouse_id) WHERE available_quantity <= reorder_point;
CREATE INDEX IF NOT EXISTS idx_warehouse_additions_warehouse ON warehouse_inventory_additions(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_additions_product ON warehouse_inventory_additions(product_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_additions_date ON warehouse_inventory_additions(receipt_date);
CREATE INDEX IF NOT EXISTS idx_warehouse_withdrawals_warehouse ON warehouse_inventory_withdrawals(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_withdrawals_product ON warehouse_inventory_withdrawals(product_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_withdrawals_date ON warehouse_inventory_withdrawals(withdrawal_date);

-- Insert sample data (optional - for development)
-- Sample warehouse
INSERT INTO warehouses (id, name, description, location, address, city, state, country, warehouse_type, created_by)
VALUES (
    uuid_generate_v4(),
    'Main Distribution Center',
    'Primary warehouse for general merchandise',
    'Industrial District, Zone A',
    '123 Warehouse Ave',
    'Metro City',
    'State',
    'Country',
    'GENERAL',
    'System'
) ON CONFLICT DO NOTHING;

COMMIT;

-- Print completion message
DO $$
BEGIN
    RAISE NOTICE 'Warehouse inventory management tables created successfully!';
    RAISE NOTICE 'Tables created: warehouses, warehouse_inventory, warehouse_inventory_additions, warehouse_inventory_withdrawals';
END $$;