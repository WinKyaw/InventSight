-- V43__migrate_sales_to_receipts.sql
-- Migrate existing sales data to the new receipts system
-- This migration populates the receipts, receipt_items, and sale_receipts tables from existing sales data

-- Step 1: Migrate existing sales to receipts table
INSERT INTO receipts (
    id,
    receipt_number,
    receipt_type,
    status,
    payment_method,
    payment_status,
    paid_amount,
    payment_date,
    subtotal,
    tax_amount,
    discount_amount,
    total_amount,
    fulfilled_by_user_id,
    fulfilled_at,
    delivery_type,
    delivery_person_id,
    delivery_assigned_at,
    delivered_at,
    delivery_notes,
    customer_id,
    customer_name,
    customer_email,
    customer_phone,
    company_id,
    store_id,
    created_by_user_id,
    created_at,
    updated_at,
    notes
)
SELECT 
    gen_random_uuid() AS id,
    s.receipt_number,
    CASE 
        WHEN s.receipt_type = 'IN_STORE' THEN 'IN_STORE'
        WHEN s.receipt_type = 'DELIVERY' THEN 'IN_STORE'  -- Map DELIVERY to IN_STORE for receipt_type
        WHEN s.receipt_type = 'PICKUP' THEN 'IN_STORE'    -- Map PICKUP to IN_STORE for receipt_type
        WHEN s.receipt_type = 'HOLD' THEN 'IN_STORE'      -- Map HOLD to IN_STORE for receipt_type
        ELSE 'IN_STORE'  -- Default to IN_STORE
    END AS receipt_type,  -- Note: delivery_type field will handle actual delivery method
    CASE 
        WHEN s.status = 'PENDING' THEN 'PENDING'
        WHEN s.status = 'COMPLETED' THEN 'COMPLETED'
        WHEN s.status = 'CANCELLED' THEN 'CANCELLED'
        ELSE 'COMPLETED'  -- Default to COMPLETED for REFUNDED and DELIVERED
    END AS status,
    s.payment_method,
    CASE 
        WHEN s.status = 'REFUNDED' THEN 'REFUNDED'
        WHEN s.payment_method IS NOT NULL THEN 'PAID'
        ELSE 'UNPAID'
    END AS payment_status,
    s.total_amount AS paid_amount,  -- Assume full payment when payment_method is set
    CASE 
        WHEN s.payment_method IS NOT NULL THEN s.created_at
        ELSE NULL
    END AS payment_date,
    s.subtotal,
    s.tax_amount,
    s.discount_amount,
    s.total_amount,
    s.fulfilled_by_user_id,
    s.fulfilled_at,
    CASE 
        WHEN s.receipt_type = 'IN_STORE' THEN 'IN_STORE'
        WHEN s.receipt_type = 'DELIVERY' THEN 'DELIVERY'
        WHEN s.receipt_type = 'PICKUP' THEN 'PICKUP'
        ELSE 'IN_STORE'  -- Default to IN_STORE if NULL or HOLD
    END AS delivery_type,
    s.delivery_person_id,
    s.delivery_assigned_at,
    s.delivered_at,
    s.delivery_notes,
    s.customer_id,
    s.customer_name,
    s.customer_email,
    s.customer_phone,
    s.company_id,
    s.store_id,
    s.user_id AS created_by_user_id,
    s.created_at,
    s.updated_at,
    s.notes
FROM sales s
WHERE s.receipt_number IS NOT NULL;

-- Step 2: Create sale_receipt mappings
-- This links sales to their corresponding receipts
INSERT INTO sale_receipts (
    receipt_id,
    sale_id,
    created_at
)
SELECT 
    r.id AS receipt_id,
    s.id AS sale_id,
    s.created_at
FROM sales s
JOIN receipts r ON r.receipt_number = s.receipt_number
WHERE s.receipt_number IS NOT NULL;

-- Step 3: Migrate sale_items to receipt_items
-- This copies line items from sales to receipts
INSERT INTO receipt_items (
    receipt_id,
    product_id,
    product_name,
    product_sku,
    quantity,
    unit_price,
    total_price,
    created_at
)
SELECT 
    r.id AS receipt_id,
    si.product_id,
    si.product_name,
    si.product_sku,
    si.quantity,
    si.unit_price,
    si.total_price,
    s.created_at
FROM sale_items si
JOIN sales s ON si.sale_id = s.id
JOIN receipts r ON r.receipt_number = s.receipt_number
WHERE s.receipt_number IS NOT NULL;

-- Verification queries (commented out - for manual verification)
-- Verify receipt count matches sales count
-- SELECT 
--     (SELECT COUNT(*) FROM receipts WHERE receipt_type = 'IN_STORE') AS receipt_count,
--     (SELECT COUNT(*) FROM sales WHERE receipt_number IS NOT NULL) AS sale_count;

-- Verify sale_receipt mappings
-- SELECT COUNT(*) FROM sale_receipts;

-- Verify receipt_items count matches sale_items count
-- SELECT 
--     (SELECT COUNT(*) FROM receipt_items) AS receipt_item_count,
--     (SELECT COUNT(*) FROM sale_items si JOIN sales s ON si.sale_id = s.id WHERE s.receipt_number IS NOT NULL) AS sale_item_count;
