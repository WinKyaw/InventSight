-- Add packing-related fields to transfer_requests
-- Date: 2026-02-05
-- Purpose: Track who packed items and when they were marked ready for pickup

ALTER TABLE transfer_requests
ADD COLUMN IF NOT EXISTS packed_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS packed_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS packing_notes TEXT;

COMMENT ON COLUMN transfer_requests.packed_by IS 'Name of person who packed the items';
COMMENT ON COLUMN transfer_requests.packed_at IS 'When items were packed and marked ready';
COMMENT ON COLUMN transfer_requests.packing_notes IS 'Notes about packing/readiness';
