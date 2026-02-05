-- Migration: Add missing status values to transfer_requests check constraint
-- Date: 2026-02-04
-- Purpose: Support full transfer workflow (READY, IN_TRANSIT, DELIVERED)
--
-- Background:
-- The transfer_requests table has a CHECK constraint that was limiting status values.
-- This constraint was preventing the use of intermediate workflow statuses like
-- READY, IN_TRANSIT, and DELIVERED which are critical for proper transfer tracking.

-- Drop existing constraint if it exists
ALTER TABLE transfer_requests 
DROP CONSTRAINT IF EXISTS transfer_requests_status_check;

-- Add new constraint with all status values
-- This supports the complete workflow:
-- PENDING → APPROVED → READY → IN_TRANSIT → DELIVERED → COMPLETED
-- Alternative paths: PENDING → REJECTED, any → CANCELLED
ALTER TABLE transfer_requests 
ADD CONSTRAINT transfer_requests_status_check 
CHECK (status IN (
  'PENDING',           -- Initial state: transfer requested
  'APPROVED',          -- GM+ approved the transfer
  'REJECTED',          -- GM+ rejected the transfer
  'PREPARING',         -- Items being prepared for shipment
  'READY',             -- Items packed and ready for pickup
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
'Valid transfer workflow statuses supporting complete transfer lifecycle from request to completion';

-- Verify constraint exists
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'transfer_requests_status_check'
  ) THEN
    RAISE EXCEPTION 'Failed to create transfer_requests_status_check constraint';
  END IF;
END $$;
