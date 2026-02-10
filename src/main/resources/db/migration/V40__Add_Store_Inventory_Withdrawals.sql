-- Create store_inventory_withdrawals table
CREATE TABLE IF NOT EXISTS store_inventory_withdrawals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Location
    store_id UUID NOT NULL,
    
    -- Product
    product_id UUID NOT NULL,
    
    -- Quantities
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    
    -- Transaction details
    transaction_type VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    reason TEXT,
    notes TEXT,
    
    -- Timestamps
    withdrawal_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Status
    status VARCHAR(50) DEFAULT 'COMPLETED',
    
    -- Audit
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    -- Foreign keys
    CONSTRAINT fk_store_withdrawal_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_store_withdrawal_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_store_withdrawals_store ON store_inventory_withdrawals(store_id);
CREATE INDEX idx_store_withdrawals_product ON store_inventory_withdrawals(product_id);
CREATE INDEX idx_store_withdrawals_date ON store_inventory_withdrawals(withdrawal_date);
CREATE INDEX idx_store_withdrawals_type ON store_inventory_withdrawals(transaction_type);
CREATE INDEX idx_store_withdrawals_reference ON store_inventory_withdrawals(reference_number);
CREATE INDEX idx_store_withdrawals_created_at ON store_inventory_withdrawals(created_at DESC);
CREATE INDEX idx_store_withdrawals_store_date ON store_inventory_withdrawals(store_id, withdrawal_date DESC);

-- Add comments
COMMENT ON TABLE store_inventory_withdrawals IS 'Tracks all inventory leaving stores (transfers, damages, losses, etc.)';
COMMENT ON COLUMN store_inventory_withdrawals.transaction_type IS 'Type: TRANSFER_OUT, DAMAGE, LOSS, RETURN_TO_SUPPLIER, ADJUSTMENT, EXPIRED, STOLEN';
COMMENT ON COLUMN store_inventory_withdrawals.reference_number IS 'References transfer_request.id, incident report, or other source';
