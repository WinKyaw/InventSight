-- V44__cleanup_sales_table.sql
-- Mark receipt-related columns in sales table as deprecated
-- These columns are now in the receipts table, accessed via the sale_receipts junction table
-- NOTE: Columns are NOT dropped yet to maintain backward compatibility during transition
-- They will be dropped in a future migration after all code is updated

-- Add comments to mark deprecated columns
COMMENT ON COLUMN sales.receipt_number IS 'DEPRECATED: Use receipts table via sale_receipts junction';
COMMENT ON COLUMN sales.status IS 'DEPRECATED: Use receipts.status via sale_receipts junction';  
COMMENT ON COLUMN sales.payment_method IS 'DEPRECATED: Use receipts.payment_method via sale_receipts junction';
COMMENT ON COLUMN sales.fulfilled_by_user_id IS 'DEPRECATED: Use receipts.fulfilled_by_user_id via sale_receipts junction';
COMMENT ON COLUMN sales.fulfilled_at IS 'DEPRECATED: Use receipts.fulfilled_at via sale_receipts junction';
COMMENT ON COLUMN sales.receipt_type IS 'DEPRECATED: Use receipts.delivery_type via sale_receipts junction';
COMMENT ON COLUMN sales.delivery_person_id IS 'DEPRECATED: Use receipts.delivery_person_id via sale_receipts junction';
COMMENT ON COLUMN sales.delivery_assigned_at IS 'DEPRECATED: Use receipts.delivery_assigned_at via sale_receipts junction';
COMMENT ON COLUMN sales.delivered_at IS 'DEPRECATED: Use receipts.delivered_at via sale_receipts junction';
COMMENT ON COLUMN sales.delivery_notes IS 'DEPRECATED: Use receipts.delivery_notes via sale_receipts junction';

-- The sales table keeps these columns for backward compatibility
-- Receipt information should be accessed via: sale -> sale_receipts -> receipts
-- Future migration will drop these columns after all code is updated

