-- Migration: Add Transfer Locations Polymorphic Relationship
-- Version: V37
-- Description: Refactor transfer location tracking to use proper polymorphic relationship pattern

-- Create transfer_locations table for polymorphic location references
CREATE TABLE IF NOT EXISTS transfer_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location_type VARCHAR(20) NOT NULL,
    
    -- Polymorphic references (exactly one must be set)
    warehouse_id UUID,
    store_id UUID,
    merchant_id UUID, -- For future use
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_transfer_location_warehouse 
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_location_store 
        FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
    
    -- Ensure exactly ONE location is set
    CONSTRAINT chk_transfer_location_exactly_one CHECK (
        (location_type = 'WAREHOUSE' AND warehouse_id IS NOT NULL AND store_id IS NULL AND merchant_id IS NULL) OR
        (location_type = 'STORE' AND warehouse_id IS NULL AND store_id IS NOT NULL AND merchant_id IS NULL) OR
        (location_type = 'MERCHANT' AND warehouse_id IS NULL AND store_id IS NULL AND merchant_id IS NOT NULL)
    )
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_transfer_locations_warehouse ON transfer_locations(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_transfer_locations_store ON transfer_locations(store_id);
CREATE INDEX IF NOT EXISTS idx_transfer_locations_type ON transfer_locations(location_type);

-- Add new columns to transfer_requests (nullable for migration)
ALTER TABLE transfer_requests
    ADD COLUMN IF NOT EXISTS from_transfer_location_id UUID,
    ADD COLUMN IF NOT EXISTS to_transfer_location_id UUID;

-- Migrate existing data: Create transfer_locations for all unique warehouses used in transfers

-- For source warehouses
INSERT INTO transfer_locations (location_type, warehouse_id)
SELECT DISTINCT 'WAREHOUSE', from_warehouse_id
FROM transfer_requests
WHERE from_warehouse_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- For destination warehouses
INSERT INTO transfer_locations (location_type, warehouse_id)
SELECT DISTINCT 'WAREHOUSE', to_warehouse_id
FROM transfer_requests
WHERE to_warehouse_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM transfer_locations tl 
    WHERE tl.warehouse_id = transfer_requests.to_warehouse_id 
      AND tl.location_type = 'WAREHOUSE'
  );

-- For source stores
INSERT INTO transfer_locations (location_type, store_id)
SELECT DISTINCT 'STORE', from_store_id
FROM transfer_requests
WHERE from_store_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- For destination stores
INSERT INTO transfer_locations (location_type, store_id)
SELECT DISTINCT 'STORE', to_store_id
FROM transfer_requests
WHERE to_store_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM transfer_locations tl 
    WHERE tl.store_id = transfer_requests.to_store_id 
      AND tl.location_type = 'STORE'
  );

-- Update transfer_requests with from_transfer_location_id (warehouse sources)
UPDATE transfer_requests tr
SET from_transfer_location_id = tl.id
FROM transfer_locations tl
WHERE tr.from_warehouse_id = tl.warehouse_id
  AND tl.location_type = 'WAREHOUSE'
  AND tr.from_transfer_location_id IS NULL
  AND tr.from_warehouse_id IS NOT NULL;

-- Update transfer_requests with from_transfer_location_id (store sources)
UPDATE transfer_requests tr
SET from_transfer_location_id = tl.id
FROM transfer_locations tl
WHERE tr.from_store_id = tl.store_id
  AND tl.location_type = 'STORE'
  AND tr.from_transfer_location_id IS NULL
  AND tr.from_store_id IS NOT NULL;

-- Update transfer_requests with to_transfer_location_id (warehouse destinations)
UPDATE transfer_requests tr
SET to_transfer_location_id = tl.id
FROM transfer_locations tl
WHERE tr.to_warehouse_id = tl.warehouse_id
  AND tl.location_type = 'WAREHOUSE'
  AND tr.to_transfer_location_id IS NULL
  AND tr.to_warehouse_id IS NOT NULL;

-- Update transfer_requests with to_transfer_location_id (store destinations)
UPDATE transfer_requests tr
SET to_transfer_location_id = tl.id
FROM transfer_locations tl
WHERE tr.to_store_id = tl.store_id
  AND tl.location_type = 'STORE'
  AND tr.to_transfer_location_id IS NULL
  AND tr.to_store_id IS NOT NULL;

-- Make new columns NOT NULL only if all records have been migrated
-- Check if all transfer_requests have valid from_transfer_location_id and to_transfer_location_id
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM transfer_requests 
    WHERE from_transfer_location_id IS NULL OR to_transfer_location_id IS NULL
  ) THEN
    ALTER TABLE transfer_requests
      ALTER COLUMN from_transfer_location_id SET NOT NULL,
      ALTER COLUMN to_transfer_location_id SET NOT NULL;
  ELSE
    RAISE NOTICE 'WARNING: Some transfer_requests do not have transfer_location_ids set. Skipping NOT NULL constraint.';
  END IF;
END $$;

-- Add foreign keys
ALTER TABLE transfer_requests
    ADD CONSTRAINT IF NOT EXISTS fk_transfer_from_location 
        FOREIGN KEY (from_transfer_location_id) REFERENCES transfer_locations(id) ON DELETE RESTRICT,
    ADD CONSTRAINT IF NOT EXISTS fk_transfer_to_location 
        FOREIGN KEY (to_transfer_location_id) REFERENCES transfer_locations(id) ON DELETE RESTRICT;

-- Create indexes on transfer_requests
CREATE INDEX IF NOT EXISTS idx_transfer_from_location ON transfer_requests(from_transfer_location_id);
CREATE INDEX IF NOT EXISTS idx_transfer_to_location ON transfer_requests(to_transfer_location_id);

-- Add comments to deprecate old columns (will be dropped in v0.3.0)
COMMENT ON COLUMN transfer_requests.from_warehouse_id IS 'DEPRECATED v0.2.1: Use from_transfer_location_id. Will be removed in v0.3.0';
COMMENT ON COLUMN transfer_requests.from_store_id IS 'DEPRECATED v0.2.1: Use from_transfer_location_id. Will be removed in v0.3.0';
COMMENT ON COLUMN transfer_requests.to_warehouse_id IS 'DEPRECATED v0.2.1: Use to_transfer_location_id. Will be removed in v0.3.0';
COMMENT ON COLUMN transfer_requests.to_store_id IS 'DEPRECATED v0.2.1: Use to_transfer_location_id. Will be removed in v0.3.0';
COMMENT ON COLUMN transfer_requests.from_location_type IS 'DEPRECATED v0.2.1: Use from_transfer_location_id->location_type. Will be removed in v0.3.0';
COMMENT ON COLUMN transfer_requests.to_location_type IS 'DEPRECATED v0.2.1: Use to_transfer_location_id->location_type. Will be removed in v0.3.0';
COMMENT ON COLUMN transfer_requests.from_location_id IS 'DEPRECATED v0.2.1: Use from_transfer_location_id. Will be removed in v0.3.0';
COMMENT ON COLUMN transfer_requests.to_location_id IS 'DEPRECATED v0.2.1: Use to_transfer_location_id. Will be removed in v0.3.0';
