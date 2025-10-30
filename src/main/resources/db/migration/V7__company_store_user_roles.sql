-- Migration V7: Add many-to-many role mapping table for CompanyStoreUser
-- Supports multiple roles per (user, company[, store]) membership

-- Create the company_store_user_roles table
CREATE TABLE company_store_user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_store_user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to company_store_user table
    CONSTRAINT fk_csur_company_store_user
        FOREIGN KEY (company_store_user_id) 
        REFERENCES company_store_user(id)
        ON DELETE CASCADE,
    
    -- Unique constraint: one role record per membership
    CONSTRAINT uk_csur_membership_role 
        UNIQUE (company_store_user_id, role)
);

-- Create indexes for performance
CREATE INDEX idx_csur_company_store_user ON company_store_user_roles(company_store_user_id);
CREATE INDEX idx_csur_role ON company_store_user_roles(role);
CREATE INDEX idx_csur_active ON company_store_user_roles(is_active);
CREATE INDEX idx_csur_membership_active ON company_store_user_roles(company_store_user_id, is_active);

-- Backfill existing roles from company_store_user table
-- For each existing membership with a role, create a corresponding role mapping entry
INSERT INTO company_store_user_roles 
    (id, company_store_user_id, role, is_active, assigned_at, assigned_by, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    csu.id,
    csu.role,
    csu.is_active,
    csu.assigned_at,
    csu.assigned_by,
    csu.created_at,
    csu.updated_at
FROM company_store_user csu
WHERE csu.role IS NOT NULL;

-- Note: The 'role' column in company_store_user is kept for backward compatibility
-- but is marked as deprecated in the code. Services should read from company_store_user_roles instead.
-- Future migration can remove the role column once all callers are updated.
