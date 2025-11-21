-- V6__sales_orders.sql
-- Migration to add sales order tables for employee sales functionality

-- Create sales_orders table
CREATE TABLE IF NOT EXISTS sales_orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    requires_manager_approval BOOLEAN DEFAULT FALSE,
    currency_code VARCHAR(3) NOT NULL,
    customer_name VARCHAR(200),
    customer_phone VARCHAR(20),
    customer_email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create index on tenant_id and status for efficient queries
CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_status ON sales_orders(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sales_orders_created_at ON sales_orders(created_at);
CREATE INDEX IF NOT EXISTS idx_sales_orders_tenant_created ON sales_orders(tenant_id, created_at);

-- Create sales_order_items table
CREATE TABLE IF NOT EXISTS sales_order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    discount_percent DECIMAL(5, 2) DEFAULT 0 CHECK (discount_percent >= 0 AND discount_percent <= 100),
    currency_code VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES sales_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_sales_order_items_order ON sales_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_sales_order_items_warehouse ON sales_order_items(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_sales_order_items_product ON sales_order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_sales_order_items_order_created ON sales_order_items(order_id, created_at);

-- Add comments for documentation
COMMENT ON TABLE sales_orders IS 'Sales orders created by employees for customer transactions';
COMMENT ON TABLE sales_order_items IS 'Line items for sales orders with product and pricing details';
COMMENT ON COLUMN sales_orders.status IS 'Order lifecycle status: DRAFT, SUBMITTED, PENDING_MANAGER_APPROVAL, CONFIRMED, CANCEL_REQUESTED, CANCELLED, FULFILLED';
COMMENT ON COLUMN sales_orders.requires_manager_approval IS 'Flag indicating if manager approval is required for this order';
COMMENT ON COLUMN sales_order_items.unit_price IS 'Sale price per unit (not cost price, employees cannot see cost)';
COMMENT ON COLUMN sales_order_items.discount_percent IS 'Discount percentage applied to this line item';
