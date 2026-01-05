-- Create table to track store inventory additions (restocks)
CREATE TABLE IF NOT EXISTS store_inventory_additions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_cost DECIMAL(19, 2),
    total_cost DECIMAL(19, 2),
    supplier_name VARCHAR(200),
    reference_number VARCHAR(100),
    receipt_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE,
    batch_number VARCHAR(100),
    notes TEXT,
    transaction_type VARCHAR(50) DEFAULT 'RESTOCK',
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    
    CONSTRAINT fk_store_inventory_addition_store 
        FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    CONSTRAINT fk_store_inventory_addition_product 
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_store_additions_store_id ON store_inventory_additions(store_id);
CREATE INDEX idx_store_additions_product_id ON store_inventory_additions(product_id);
CREATE INDEX idx_store_additions_receipt_date ON store_inventory_additions(receipt_date);
CREATE INDEX idx_store_additions_created_by ON store_inventory_additions(created_by);
CREATE INDEX idx_store_additions_created_at ON store_inventory_additions(created_at DESC);
CREATE INDEX idx_store_additions_store_date ON store_inventory_additions(store_id, receipt_date DESC);
