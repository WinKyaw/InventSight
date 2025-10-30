-- Migration V7: Add many-to-many role mapping table for company_store_user
-- This enables multiple roles per user membership while maintaining backward compatibility

-- Create the company_store_user_roles table
CREATE TABLE IF NOT EXISTS company_store_user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_store_user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_store_user 
        FOREIGN KEY (company_store_user_id) 
        REFERENCES company_store_user(id) 
        ON DELETE CASCADE,
    CONSTRAINT unique_company_store_user_role 
        UNIQUE (company_store_user_id, role)
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_company_store_user_roles_membership 
    ON company_store_user_roles(company_store_user_id);

CREATE INDEX IF NOT EXISTS idx_company_store_user_roles_active 
    ON company_store_user_roles(company_store_user_id, is_active);

-- Backfill: Migrate existing roles from company_store_user.role to company_store_user_roles
-- Only backfill for active memberships with non-null roles
INSERT INTO company_store_user_roles (
    company_store_user_id,
    role,
    is_active,
    assigned_at,
    assigned_by,
    created_at,
    updated_at
)
SELECT 
    csu.id,
    csu.role,
    csu.is_active,
    csu.assigned_at,
    csu.assigned_by,
    csu.created_at,
    csu.updated_at
FROM company_store_user csu
WHERE csu.role IS NOT NULL
ON CONFLICT (company_store_user_id, role) DO NOTHING;

-- Note: The role column in company_store_user is kept for backward compatibility
-- but is now considered deprecated. Services will read from company_store_user_roles instead.
-- A future migration can remove the role column from company_store_user once all callers are updated.
