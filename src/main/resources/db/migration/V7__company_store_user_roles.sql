-- V7__company_store_user_roles.sql
-- Migration to add many-to-many role mapping for company/store users
-- This allows a single user to hold multiple roles within a company or store

-- Create company_store_user_roles table
CREATE TABLE IF NOT EXISTS company_store_user_roles (
    id UUID PRIMARY KEY,
    company_store_user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_store_user_id) REFERENCES company_store_user(id) ON DELETE CASCADE,
    UNIQUE (company_store_user_id, role)
);

-- Create indexes for efficient queries
CREATE INDEX idx_company_store_user_roles_user ON company_store_user_roles(company_store_user_id);
CREATE INDEX idx_company_store_user_roles_role ON company_store_user_roles(role);
CREATE INDEX idx_company_store_user_roles_active ON company_store_user_roles(is_active);
CREATE INDEX idx_company_store_user_roles_user_active ON company_store_user_roles(company_store_user_id, is_active);

-- Backfill existing roles from company_store_user table
-- This preserves all existing role assignments by copying them to the new mapping table
INSERT INTO company_store_user_roles (
    id, 
    company_store_user_id, 
    role, 
    is_active, 
    assigned_at, 
    assigned_by, 
    created_at, 
    updated_at
)
SELECT 
    gen_random_uuid() as id,
    csu.id as company_store_user_id,
    csu.role as role,
    csu.is_active as is_active,
    csu.assigned_at as assigned_at,
    csu.assigned_by as assigned_by,
    csu.created_at as created_at,
    csu.updated_at as updated_at
FROM company_store_user csu
WHERE csu.role IS NOT NULL
ON CONFLICT (company_store_user_id, role) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE company_store_user_roles IS 'Many-to-many mapping allowing users to have multiple roles per company/store membership';
COMMENT ON COLUMN company_store_user_roles.company_store_user_id IS 'Foreign key to company_store_user table';
COMMENT ON COLUMN company_store_user_roles.role IS 'Role assigned: FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER, or EMPLOYEE';
COMMENT ON COLUMN company_store_user_roles.is_active IS 'Whether this role assignment is currently active';
COMMENT ON COLUMN company_store_user_roles.assigned_at IS 'Timestamp when the role was assigned';
COMMENT ON COLUMN company_store_user_roles.assigned_by IS 'Username of the person who assigned this role';
COMMENT ON COLUMN company_store_user_roles.revoked_at IS 'Timestamp when the role was revoked (if applicable)';
COMMENT ON COLUMN company_store_user_roles.revoked_by IS 'Username of the person who revoked this role';

-- Note: The 'role' column in company_store_user table is kept for backward compatibility
-- but will be deprecated in favor of the new mapping table
-- A future migration can remove it once all code is updated to use the mapping table
