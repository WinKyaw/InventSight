-- Create warehouse_permissions table
CREATE TABLE warehouse_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_type VARCHAR(20) NOT NULL CHECK (permission_type IN ('READ', 'READ_WRITE')),
    granted_by VARCHAR(255),
    granted_at TIMESTAMP,
    revoked_by VARCHAR(255),
    revoked_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create partial unique constraint: only one active permission per user per warehouse
CREATE UNIQUE INDEX idx_warehouse_permissions_active_unique 
    ON warehouse_permissions(warehouse_id, user_id) 
    WHERE is_active = true;

-- Create indexes
CREATE INDEX idx_warehouse_permissions_warehouse ON warehouse_permissions(warehouse_id);
CREATE INDEX idx_warehouse_permissions_user ON warehouse_permissions(user_id);
CREATE INDEX idx_warehouse_permissions_active ON warehouse_permissions(is_active);

-- Add comments
COMMENT ON TABLE warehouse_permissions IS 'Stores user-specific permissions for warehouses';
COMMENT ON COLUMN warehouse_permissions.permission_type IS 'READ = view only, READ_WRITE = view + add/withdraw';
COMMENT ON COLUMN warehouse_permissions.is_active IS 'False if permission has been revoked';
