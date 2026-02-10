-- Migration: Refactor Transfer Locations to Route-Based Model
-- Version: V39
-- Description: Simplify transfer_locations to represent complete routes (from → to)
--              instead of separate source/destination references
--
-- SAFETY: This migration is designed for safe rollback
-- - Phase 1: Add new columns (nullable)
-- - Phase 2: Migrate data
-- - Phase 3: Validate migration
-- - Phase 4: Apply constraints and drop old columns

-- ============================================================================
-- PHASE 1: Add new columns to transfer_locations (nullable for migration)
-- ============================================================================

ALTER TABLE transfer_locations
    ADD COLUMN IF NOT EXISTS from_id UUID,
    ADD COLUMN IF NOT EXISTS from_location_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS from_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS to_id UUID,
    ADD COLUMN IF NOT EXISTS to_location_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS to_name VARCHAR(255);

-- ============================================================================
-- PHASE 2: Migrate existing data
-- ============================================================================

-- Step 1: Create a mapping table to track from/to location pairs from existing transfer_requests
-- This helps us identify which locations form routes

CREATE TEMP TABLE temp_transfer_routes AS
SELECT DISTINCT
    tr.from_transfer_location_id as from_loc_id,
    tr.to_transfer_location_id as to_loc_id,
    fl.location_type as from_type,
    fl.warehouse_id as from_warehouse_id,
    fl.store_id as from_store_id,
    tl.location_type as to_type,
    tl.warehouse_id as to_warehouse_id,
    tl.store_id as to_store_id
FROM transfer_requests tr
INNER JOIN transfer_locations fl ON tr.from_transfer_location_id = fl.id
INNER JOIN transfer_locations tl ON tr.to_transfer_location_id = tl.id
WHERE tr.from_transfer_location_id IS NOT NULL 
  AND tr.to_transfer_location_id IS NOT NULL;

-- Step 2: Create new transfer_location records for each unique route
-- We'll create a new record that represents the complete route

INSERT INTO transfer_locations (from_id, from_location_type, from_name, to_id, to_location_type, to_name, created_at)
SELECT DISTINCT
    CASE 
        WHEN temp.from_type = 'WAREHOUSE' THEN temp.from_warehouse_id
        WHEN temp.from_type = 'STORE' THEN temp.from_store_id
    END as from_id,
    temp.from_type as from_location_type,
    CASE 
        WHEN temp.from_type = 'WAREHOUSE' THEN (SELECT name FROM warehouses WHERE id = temp.from_warehouse_id)
        WHEN temp.from_type = 'STORE' THEN (SELECT store_name FROM stores WHERE id = temp.from_store_id)
    END as from_name,
    CASE 
        WHEN temp.to_type = 'WAREHOUSE' THEN temp.to_warehouse_id
        WHEN temp.to_type = 'STORE' THEN temp.to_store_id
    END as to_id,
    temp.to_type as to_location_type,
    CASE 
        WHEN temp.to_type = 'WAREHOUSE' THEN (SELECT name FROM warehouses WHERE id = temp.to_warehouse_id)
        WHEN temp.to_type = 'STORE' THEN (SELECT store_name FROM stores WHERE id = temp.to_store_id)
    END as to_name,
    CURRENT_TIMESTAMP as created_at
FROM temp_transfer_routes temp
WHERE NOT EXISTS (
    SELECT 1 FROM transfer_locations tl2
    WHERE tl2.from_id = CASE 
            WHEN temp.from_type = 'WAREHOUSE' THEN temp.from_warehouse_id
            WHEN temp.from_type = 'STORE' THEN temp.from_store_id
        END
    AND tl2.from_location_type = temp.from_type
    AND tl2.to_id = CASE 
            WHEN temp.to_type = 'WAREHOUSE' THEN temp.to_warehouse_id
            WHEN temp.to_type = 'STORE' THEN temp.to_store_id
        END
    AND tl2.to_location_type = temp.to_type
    AND tl2.from_id IS NOT NULL
    AND tl2.to_id IS NOT NULL
);

-- Step 3: Add transfer_location_id column to transfer_requests (nullable initially)
ALTER TABLE transfer_requests
    ADD COLUMN IF NOT EXISTS transfer_location_id UUID;

-- Step 4: Update transfer_requests to reference the new route-based transfer_locations
UPDATE transfer_requests tr
SET transfer_location_id = (
    SELECT tl_new.id
    FROM transfer_locations fl
    INNER JOIN transfer_locations tol ON TRUE
    INNER JOIN transfer_locations tl_new ON 
        tl_new.from_id = CASE 
            WHEN fl.location_type = 'WAREHOUSE' THEN fl.warehouse_id
            WHEN fl.location_type = 'STORE' THEN fl.store_id
        END
        AND tl_new.from_location_type = fl.location_type
        AND tl_new.to_id = CASE 
            WHEN tol.location_type = 'WAREHOUSE' THEN tol.warehouse_id
            WHEN tol.location_type = 'STORE' THEN tol.store_id
        END
        AND tl_new.to_location_type = tol.location_type
    WHERE fl.id = tr.from_transfer_location_id
      AND tol.id = tr.to_transfer_location_id
      AND tl_new.from_id IS NOT NULL
      AND tl_new.to_id IS NOT NULL
    LIMIT 1
)
WHERE tr.from_transfer_location_id IS NOT NULL 
  AND tr.to_transfer_location_id IS NOT NULL
  AND tr.transfer_location_id IS NULL;

-- ============================================================================
-- PHASE 3: Validate migration
-- ============================================================================

-- Log statistics for verification
DO $$
DECLARE
    total_requests INTEGER;
    migrated_requests INTEGER;
    unmigrated_requests INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_requests FROM transfer_requests;
    SELECT COUNT(*) INTO migrated_requests FROM transfer_requests WHERE transfer_location_id IS NOT NULL;
    SELECT COUNT(*) INTO unmigrated_requests FROM transfer_requests WHERE transfer_location_id IS NULL;
    
    RAISE NOTICE '=== Migration Statistics ===';
    RAISE NOTICE 'Total transfer requests: %', total_requests;
    RAISE NOTICE 'Migrated to new schema: %', migrated_requests;
    RAISE NOTICE 'Unmigrated (will use nullable): %', unmigrated_requests;
    
    IF unmigrated_requests > 0 THEN
        RAISE NOTICE 'WARNING: % requests not migrated - keeping columns nullable', unmigrated_requests;
    END IF;
END $$;

-- ============================================================================
-- PHASE 4: Apply constraints and clean up
-- ============================================================================

-- Only apply NOT NULL if ALL records are migrated
DO $$
DECLARE
    unmigrated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unmigrated_count 
    FROM transfer_requests 
    WHERE transfer_location_id IS NULL 
      AND from_transfer_location_id IS NOT NULL;
    
    IF unmigrated_count = 0 THEN
        -- All records migrated successfully - safe to add constraint
        ALTER TABLE transfer_requests
            ALTER COLUMN transfer_location_id SET NOT NULL;
        RAISE NOTICE '✅ Added NOT NULL constraint to transfer_location_id';
    ELSE
        RAISE NOTICE '⚠️  Skipping NOT NULL constraint - % unmigrated records exist', unmigrated_count;
    END IF;
END $$;

-- Add foreign key constraint
ALTER TABLE transfer_requests
    ADD CONSTRAINT IF NOT EXISTS fk_transfer_location 
        FOREIGN KEY (transfer_location_id) 
        REFERENCES transfer_locations(id) 
        ON DELETE RESTRICT;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_transfer_requests_location ON transfer_requests(transfer_location_id);

-- Add constraint to ensure from and to are different
ALTER TABLE transfer_locations
    ADD CONSTRAINT IF NOT EXISTS chk_transfer_different_locations CHECK (
        (from_id IS NULL AND to_id IS NULL) OR  -- Allow old records
        (from_id IS NOT NULL AND to_id IS NOT NULL AND 
         (from_id != to_id OR from_location_type != to_location_type))
    );

-- Add indexes on new columns for query performance
CREATE INDEX IF NOT EXISTS idx_transfer_locations_from ON transfer_locations(from_location_type, from_id) 
    WHERE from_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transfer_locations_to ON transfer_locations(to_location_type, to_id) 
    WHERE to_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transfer_locations_route ON transfer_locations(from_id, from_location_type, to_id, to_location_type) 
    WHERE from_id IS NOT NULL AND to_id IS NOT NULL;

-- ============================================================================
-- PHASE 5: Drop old columns (commented out for safety - uncomment when ready)
-- ============================================================================

-- After confirming the migration is successful, uncomment these to clean up:

-- -- Drop old foreign keys first
-- ALTER TABLE transfer_requests DROP CONSTRAINT IF EXISTS fk_transfer_from_location;
-- ALTER TABLE transfer_requests DROP CONSTRAINT IF EXISTS fk_transfer_to_location;

-- -- Drop old columns from transfer_requests
-- ALTER TABLE transfer_requests DROP COLUMN IF EXISTS from_transfer_location_id;
-- ALTER TABLE transfer_requests DROP COLUMN IF EXISTS to_transfer_location_id;
-- ALTER TABLE transfer_requests DROP COLUMN IF EXISTS from_warehouse_id;
-- ALTER TABLE transfer_requests DROP COLUMN IF EXISTS from_store_id;
-- ALTER TABLE transfer_requests DROP COLUMN IF EXISTS to_warehouse_id;
-- ALTER TABLE transfer_requests DROP COLUMN IF EXISTS to_store_id;

-- -- Drop old columns from transfer_locations
-- ALTER TABLE transfer_locations DROP CONSTRAINT IF EXISTS chk_transfer_location_exactly_one;
-- ALTER TABLE transfer_locations DROP CONSTRAINT IF EXISTS fk_transfer_location_warehouse;
-- ALTER TABLE transfer_locations DROP CONSTRAINT IF EXISTS fk_transfer_location_store;
-- ALTER TABLE transfer_locations DROP COLUMN IF EXISTS warehouse_id;
-- ALTER TABLE transfer_locations DROP COLUMN IF EXISTS store_id;
-- ALTER TABLE transfer_locations DROP COLUMN IF EXISTS merchant_id;
-- ALTER TABLE transfer_locations DROP COLUMN IF EXISTS location_type;

-- Add comments to document the schema change
COMMENT ON TABLE transfer_locations IS 'Transfer routes representing from-location → to-location pairs (refactored in V39)';
COMMENT ON COLUMN transfer_locations.from_id IS 'Source location UUID (warehouse, store, or merchant)';
COMMENT ON COLUMN transfer_locations.from_location_type IS 'Source location type: WAREHOUSE, STORE, or MERCHANT';
COMMENT ON COLUMN transfer_locations.from_name IS 'Denormalized source location name for quick display';
COMMENT ON COLUMN transfer_locations.to_id IS 'Destination location UUID (warehouse, store, or merchant)';
COMMENT ON COLUMN transfer_locations.to_location_type IS 'Destination location type: WAREHOUSE, STORE, or MERCHANT';
COMMENT ON COLUMN transfer_locations.to_name IS 'Denormalized destination location name for quick display';
COMMENT ON COLUMN transfer_requests.transfer_location_id IS 'Reference to complete transfer route (from → to)';

-- Mark old columns as deprecated (for documentation)
COMMENT ON COLUMN transfer_requests.from_transfer_location_id IS 'DEPRECATED v39: Use transfer_location_id. Will be removed in next version.';
COMMENT ON COLUMN transfer_requests.to_transfer_location_id IS 'DEPRECATED v39: Use transfer_location_id. Will be removed in next version.';

-- ============================================================================
-- ROLLBACK INSTRUCTIONS (if needed)
-- ============================================================================
-- 
-- To rollback this migration:
-- 1. Restore from_transfer_location_id and to_transfer_location_id in transfer_requests
-- 2. Drop transfer_location_id column
-- 3. Drop new route-based transfer_locations records (where from_id IS NOT NULL)
-- 4. Restore old constraints
--
-- DO NOT ROLLBACK if you have already applied code changes that depend on the new schema!
