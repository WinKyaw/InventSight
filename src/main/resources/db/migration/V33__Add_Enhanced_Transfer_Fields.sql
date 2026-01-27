-- Enhanced Transfer Request System Migration
-- Adds new fields to support all transfer scenarios (Store<->Store, Warehouse<->Warehouse, Store<->Warehouse)

-- Add location flexibility fields
ALTER TABLE transfer_requests 
  ADD COLUMN from_location_type VARCHAR(20),
  ADD COLUMN from_location_id UUID,
  ADD COLUMN to_location_type VARCHAR(20),
  ADD COLUMN to_location_id UUID;

-- Add item details fields
ALTER TABLE transfer_requests 
  ADD COLUMN item_name VARCHAR(255),
  ADD COLUMN item_sku VARCHAR(100);

-- Add carrier and delivery tracking fields
ALTER TABLE transfer_requests 
  ADD COLUMN carrier_name VARCHAR(200),
  ADD COLUMN carrier_phone VARCHAR(20),
  ADD COLUMN carrier_vehicle VARCHAR(100),
  ADD COLUMN shipped_at TIMESTAMP,
  ADD COLUMN estimated_delivery_at TIMESTAMP;

-- Add receipt tracking fields
ALTER TABLE transfer_requests 
  ADD COLUMN received_by_user_id UUID,
  ADD COLUMN receiver_name VARCHAR(200),
  ADD COLUMN received_at TIMESTAMP,
  ADD COLUMN received_quantity INTEGER,
  ADD COLUMN receipt_notes TEXT,
  ADD COLUMN is_receipt_confirmed BOOLEAN DEFAULT false;

-- Migrate existing data to use new location fields
UPDATE transfer_requests 
SET 
  from_location_type = 'WAREHOUSE',
  from_location_id = from_warehouse_id,
  to_location_type = 'STORE',
  to_location_id = to_store_id
WHERE from_location_type IS NULL;

-- Make legacy columns nullable to support new transfer types (Store<->Store, Warehouse<->Warehouse, Store<->Warehouse)
ALTER TABLE transfer_requests 
  ALTER COLUMN from_warehouse_id DROP NOT NULL,
  ALTER COLUMN to_store_id DROP NOT NULL;

-- Add indexes for performance
CREATE INDEX idx_transfer_from_location ON transfer_requests(from_location_type, from_location_id);
CREATE INDEX idx_transfer_to_location ON transfer_requests(to_location_type, to_location_id);
CREATE INDEX idx_transfer_requested_by ON transfer_requests(requested_by);
CREATE INDEX idx_transfer_dates ON transfer_requests(requested_at, shipped_at, received_at);

-- Add foreign key constraint for received_by_user_id
ALTER TABLE transfer_requests 
  ADD CONSTRAINT fk_transfer_receiver FOREIGN KEY (received_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Note: We keep the old from_warehouse_id and to_store_id columns for backward compatibility
-- They can be removed in a future migration after all code is updated
