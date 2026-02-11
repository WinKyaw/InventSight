-- V38__add_hold_to_receipt_type_constraint.sql
-- Add HOLD to the receipt_type CHECK constraint
-- Date: 2026-02-11
-- Issue: HOLD status missing from check constraint on sales table
-- Root Cause: V30 migration added receipt_type column but PostgreSQL/ORM created a constraint without HOLD
-- Solution: Drop and recreate constraint with all receipt_type values including HOLD

-- Drop the existing CHECK constraint if it exists
ALTER TABLE sales DROP CONSTRAINT IF EXISTS sales_receipt_type_check;

-- Add updated CHECK constraint that includes HOLD
ALTER TABLE sales 
ADD CONSTRAINT sales_receipt_type_check 
CHECK (receipt_type IN ('IN_STORE', 'DELIVERY', 'PICKUP', 'HOLD'));

-- Add comment for documentation
COMMENT ON CONSTRAINT sales_receipt_type_check ON sales IS 
'Validates receipt_type values: IN_STORE (immediate purchase), DELIVERY (requires delivery), PICKUP (customer pickup), HOLD (order on hold/pending)';
