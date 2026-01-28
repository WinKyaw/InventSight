-- Add comprehensive tracking fields to transfer_requests table for multi-tenant inventory transfers

-- === MULTI-TENANT COMPANY TRACKING ===
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS from_company_id UUID;
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS to_company_id UUID;

-- === ADDITIONAL LOCATION TRACKING ===
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS from_store_id UUID;
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS to_warehouse_id UUID;

-- === PEOPLE TRACKING ===
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS requested_by_user_id UUID;
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS requested_by_name VARCHAR(255);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS approved_by_user_id UUID;
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS approved_by_name VARCHAR(255);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS carrier_user_id UUID;
-- carrier_name, carrier_phone, carrier_vehicle already exist
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS receiver_user_id UUID;
-- receiver_name already exists
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS handler_user_id UUID;
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS handler_name VARCHAR(255);

-- === PRODUCT INFO ===
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS product_sku VARCHAR(100);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS damaged_quantity INTEGER DEFAULT 0;

-- === TRANSPORT & STATUS ===
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS transport_method VARCHAR(50);

-- === PROOF & SIGNATURE ===
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS receiver_signature_url VARCHAR(500);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS proof_of_delivery_url VARCHAR(500);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS condition_on_arrival VARCHAR(50);

-- === AUDIT ===
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE transfer_requests ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

-- === FOREIGN KEY CONSTRAINTS ===
-- Add foreign key constraints for company tracking (if companies table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'companies') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_transfer_from_company') THEN
            ALTER TABLE transfer_requests ADD CONSTRAINT fk_transfer_from_company FOREIGN KEY (from_company_id) REFERENCES companies(id);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_transfer_to_company') THEN
            ALTER TABLE transfer_requests ADD CONSTRAINT fk_transfer_to_company FOREIGN KEY (to_company_id) REFERENCES companies(id);
        END IF;
    END IF;
END $$;

-- === PERFORMANCE INDEXES ===
CREATE INDEX IF NOT EXISTS idx_transfer_from_company ON transfer_requests(from_company_id);
CREATE INDEX IF NOT EXISTS idx_transfer_to_company ON transfer_requests(to_company_id);
CREATE INDEX IF NOT EXISTS idx_transfer_status ON transfer_requests(status);
CREATE INDEX IF NOT EXISTS idx_transfer_requested_at ON transfer_requests(requested_at DESC);
CREATE INDEX IF NOT EXISTS idx_transfer_product ON transfer_requests(product_id);
CREATE INDEX IF NOT EXISTS idx_transfer_from_store ON transfer_requests(from_store_id);
CREATE INDEX IF NOT EXISTS idx_transfer_from_warehouse ON transfer_requests(from_warehouse_id);
