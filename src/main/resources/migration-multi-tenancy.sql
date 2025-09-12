-- Multi-tenancy, RBAC, and Tiered Pricing Migration
-- InventSight Database Migration Script
-- Generated: 2025-01-27
-- Description: Adds support for multi-tenancy, role-based access control, and tiered pricing

-- Create Stores table (master tenants table)
CREATE TABLE IF NOT EXISTS stores (
    id BIGSERIAL PRIMARY KEY,
    store_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    address VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(200),
    tax_id VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create UserStoreRole table (user-store-role mapping)
CREATE TABLE IF NOT EXISTS user_store_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    UNIQUE(user_id, store_id)
);

-- Create DiscountAuditLog table
CREATE TABLE IF NOT EXISTS discount_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL,
    store_id BIGINT NOT NULL REFERENCES stores(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    attempted_price DECIMAL(10,2) NOT NULL CHECK (attempted_price >= 0),
    original_price DECIMAL(10,2) NOT NULL CHECK (original_price >= 0),
    result VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    approved_by VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(100)
);

-- Add store_id to existing tables for multi-tenancy

-- Add store_id to products table
ALTER TABLE products ADD COLUMN IF NOT EXISTS store_id BIGINT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS owner_set_sell_price DECIMAL(10,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS retail_price DECIMAL(10,2);

-- Add store_id to employees table
ALTER TABLE employees ADD COLUMN IF NOT EXISTS store_id BIGINT;

-- Add store_id to sales table
ALTER TABLE sales ADD COLUMN IF NOT EXISTS store_id BIGINT;

-- Add store_id to events table (if it exists)
ALTER TABLE events ADD COLUMN IF NOT EXISTS store_id BIGINT;

-- Add foreign key constraints after adding columns
DO $$
BEGIN
    -- Add foreign key constraint for products.store_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_products_store_id' 
        AND table_name = 'products'
    ) THEN
        ALTER TABLE products ADD CONSTRAINT fk_products_store_id 
        FOREIGN KEY (store_id) REFERENCES stores(id);
    END IF;

    -- Add foreign key constraint for employees.store_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_employees_store_id' 
        AND table_name = 'employees'
    ) THEN
        ALTER TABLE employees ADD CONSTRAINT fk_employees_store_id 
        FOREIGN KEY (store_id) REFERENCES stores(id);
    END IF;

    -- Add foreign key constraint for sales.store_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_sales_store_id' 
        AND table_name = 'sales'
    ) THEN
        ALTER TABLE sales ADD CONSTRAINT fk_sales_store_id 
        FOREIGN KEY (store_id) REFERENCES stores(id);
    END IF;

    -- Add foreign key constraint for events.store_id (if events table exists)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'events') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints 
            WHERE constraint_name = 'fk_events_store_id' 
            AND table_name = 'events'
        ) THEN
            ALTER TABLE events ADD CONSTRAINT fk_events_store_id 
            FOREIGN KEY (store_id) REFERENCES stores(id);
        END IF;
    END IF;
END $$;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_stores_name ON stores(store_name);
CREATE INDEX IF NOT EXISTS idx_stores_active ON stores(is_active);
CREATE INDEX IF NOT EXISTS idx_stores_country ON stores(country);
CREATE INDEX IF NOT EXISTS idx_stores_state ON stores(state);

CREATE INDEX IF NOT EXISTS idx_user_store_roles_user ON user_store_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_store_roles_store ON user_store_roles(store_id);
CREATE INDEX IF NOT EXISTS idx_user_store_roles_role ON user_store_roles(role);
CREATE INDEX IF NOT EXISTS idx_user_store_roles_active ON user_store_roles(is_active);

CREATE INDEX IF NOT EXISTS idx_discount_audit_user ON discount_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_discount_audit_store ON discount_audit_log(store_id);
CREATE INDEX IF NOT EXISTS idx_discount_audit_product ON discount_audit_log(product_id);
CREATE INDEX IF NOT EXISTS idx_discount_audit_timestamp ON discount_audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_discount_audit_result ON discount_audit_log(result);
CREATE INDEX IF NOT EXISTS idx_discount_audit_session ON discount_audit_log(session_id);

-- Add multi-tenancy indexes
CREATE INDEX IF NOT EXISTS idx_products_store ON products(store_id);
CREATE INDEX IF NOT EXISTS idx_employees_store ON employees(store_id);
CREATE INDEX IF NOT EXISTS idx_sales_store ON sales(store_id);

-- Insert default store for migration compatibility
INSERT INTO stores (store_name, description, address, city, state, country, created_by) 
VALUES (
    'Default Store', 
    'Default store for existing data migration', 
    '123 Main Street', 
    'Default City', 
    'Default State', 
    'Default Country',
    'SYSTEM'
) ON CONFLICT DO NOTHING;

-- Get the default store ID for data migration
DO $$
DECLARE
    default_store_id BIGINT;
BEGIN
    SELECT id INTO default_store_id FROM stores WHERE store_name = 'Default Store';
    
    -- Update existing products to have store_id
    UPDATE products 
    SET store_id = default_store_id,
        original_price = COALESCE(price, 0),
        owner_set_sell_price = COALESCE(price, 0),
        retail_price = COALESCE(price, 0)
    WHERE store_id IS NULL;
    
    -- Update existing employees to have store_id
    UPDATE employees 
    SET store_id = default_store_id 
    WHERE store_id IS NULL;
    
    -- Update existing sales to have store_id
    UPDATE sales 
    SET store_id = default_store_id 
    WHERE store_id IS NULL;
    
    -- Update existing events to have store_id (if events table exists)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'events') THEN
        EXECUTE 'UPDATE events SET store_id = $1 WHERE store_id IS NULL' USING default_store_id;
    END IF;
    
    -- Create default admin user-store role
    INSERT INTO user_store_roles (user_id, store_id, role, assigned_by)
    SELECT u.id, default_store_id, 'OWNER', 'SYSTEM'
    FROM users u 
    WHERE u.username = 'winkyaw' 
    AND NOT EXISTS (
        SELECT 1 FROM user_store_roles usr 
        WHERE usr.user_id = u.id AND usr.store_id = default_store_id
    );
END $$;

-- Make store_id NOT NULL after migration (for new records only)
-- Note: We don't make existing columns NOT NULL to avoid breaking changes

-- Create views for easier multi-tenant queries
CREATE OR REPLACE VIEW active_stores AS
SELECT * FROM stores WHERE is_active = true;

CREATE OR REPLACE VIEW user_store_permissions AS
SELECT 
    u.id as user_id,
    u.username,
    u.email,
    s.id as store_id,
    s.store_name,
    usr.role,
    usr.is_active as role_active,
    usr.assigned_at
FROM users u
JOIN user_store_roles usr ON u.id = usr.user_id
JOIN stores s ON usr.store_id = s.id
WHERE u.is_active = true AND s.is_active = true AND usr.is_active = true;

COMMIT;

-- Log migration completion
SELECT 'Multi-tenancy, RBAC, and Tiered Pricing migration completed successfully' as migration_status;