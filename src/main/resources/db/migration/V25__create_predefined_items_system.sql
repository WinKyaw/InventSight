-- Create predefined_items table
CREATE TABLE predefined_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Item Details
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100),
    category VARCHAR(100),
    unit_type VARCHAR(50) NOT NULL,
    description TEXT,
    default_price DECIMAL(10, 2),
    
    -- Multi-tenant Fields
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID REFERENCES users(id),
    
    -- Constraints
    CONSTRAINT unique_item_per_company UNIQUE (company_id, name, unit_type)
);

-- Indexes for predefined_items
CREATE INDEX idx_predefined_items_company ON predefined_items(company_id);
CREATE INDEX idx_predefined_items_active ON predefined_items(is_active);
CREATE INDEX idx_predefined_items_name ON predefined_items(name);
CREATE INDEX idx_predefined_items_category ON predefined_items(category);

-- Create predefined_item_stores table
CREATE TABLE predefined_item_stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    predefined_item_id UUID NOT NULL REFERENCES predefined_items(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    
    CONSTRAINT unique_item_store UNIQUE (predefined_item_id, store_id)
);

CREATE INDEX idx_predefined_item_stores_item ON predefined_item_stores(predefined_item_id);
CREATE INDEX idx_predefined_item_stores_store ON predefined_item_stores(store_id);

-- Create predefined_item_warehouses table
CREATE TABLE predefined_item_warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    predefined_item_id UUID NOT NULL REFERENCES predefined_items(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    
    CONSTRAINT unique_item_warehouse UNIQUE (predefined_item_id, warehouse_id)
);

CREATE INDEX idx_predefined_item_warehouses_item ON predefined_item_warehouses(predefined_item_id);
CREATE INDEX idx_predefined_item_warehouses_warehouse ON predefined_item_warehouses(warehouse_id);

-- Create supply_management_permissions table
CREATE TABLE supply_management_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    
    permission_type VARCHAR(50) DEFAULT 'SUPPLY_MANAGEMENT_SPECIALIST',
    
    is_permanent BOOLEAN DEFAULT true,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    
    granted_by UUID NOT NULL REFERENCES users(id),
    revoked_at TIMESTAMP,
    revoked_by UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT true,
    
    notes TEXT,
    
    CONSTRAINT unique_user_company_permission UNIQUE (user_id, company_id, permission_type)
);

CREATE INDEX idx_supply_permissions_user ON supply_management_permissions(user_id);
CREATE INDEX idx_supply_permissions_company ON supply_management_permissions(company_id);
CREATE INDEX idx_supply_permissions_active ON supply_management_permissions(is_active);
