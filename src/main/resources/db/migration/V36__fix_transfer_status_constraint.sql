-- Fix transfer_requests status constraint to include READY
-- Date: 2026-02-06
-- Issue: READY status missing from check constraint in production database
-- Root Cause: V34 migration may not have been applied successfully
-- Solution: Recreate constraint with all status values

-- Drop existing constraint
ALTER TABLE transfer_requests 
DROP CONSTRAINT IF EXISTS transfer_requests_status_check;

-- Add new constraint with ALL status values including READY
ALTER TABLE transfer_requests 
ADD CONSTRAINT transfer_requests_status_check 
CHECK (status IN (
  'PENDING',           -- Initial state: transfer requested
  'APPROVED',          -- GM+ approved the transfer
  'REJECTED',          -- GM+ rejected the transfer
  'PREPARING',         -- Items being prepared for shipment
  'READY',             -- ✅ Items packed and ready for pickup
  'IN_TRANSIT',        -- Delivery in progress
  'DELIVERED',         -- Items delivered to destination
  'RECEIVED',          -- Confirmed received (partial or full)
  'PARTIALLY_RECEIVED',-- Some items received
  'COMPLETED',         -- Receipt confirmed, inventory updated
  'CANCELLED',         -- Transfer cancelled
  'DAMAGED',           -- Arrived damaged
  'LOST'               -- Lost in transit
));

-- Add comment for documentation
COMMENT ON CONSTRAINT transfer_requests_status_check ON transfer_requests IS 
'Valid transfer workflow statuses including READY for packed items awaiting pickup';

-- Verify constraint was created successfully
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'transfer_requests_status_check'
    AND contype = 'c'
  ) THEN
    RAISE EXCEPTION 'Failed to create transfer_requests_status_check constraint';
  END IF;
  
  RAISE NOTICE 'Successfully updated transfer_requests_status_check constraint with READY status';
END $$;
