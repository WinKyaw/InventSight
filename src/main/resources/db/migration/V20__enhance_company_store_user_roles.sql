-- Migration V20: Enhance company_store_user_roles table with direct FKs and fix FOUNDER bug
-- Add user_id, company_id, store_id for direct lookups without joining through company_store_user
-- Add expires_at and permanent columns for role management

-- Step 1: Add new columns to company_store_user_roles
ALTER TABLE company_store_user_roles
ADD COLUMN IF NOT EXISTS user_id UUID,
ADD COLUMN IF NOT EXISTS company_id UUID,
ADD COLUMN IF NOT EXISTS store_id UUID,
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS permanent BOOLEAN DEFAULT FALSE;

-- Step 2: Backfill user_id, company_id, store_id from company_store_user table
UPDATE company_store_user_roles csur
SET 
    user_id = csu.user_id,
    company_id = csu.company_id,
    store_id = csu.store_id
FROM company_store_user csu
WHERE csur.company_store_user_id = csu.id
AND csur.user_id IS NULL;

-- Step 3: Fix FOUNDER bug and set permanent flag
-- If users.role = 'OWNER', the role should be mapped to CompanyRole based on actual user role
-- If users.role = 'EMPLOYEE', role should be 'EMPLOYEE' (NOT FOUNDER!)
UPDATE company_store_user_roles csur
SET 
    permanent = CASE 
        WHEN u.role IN ('OWNER', 'CO_OWNER', 'ADMIN') THEN TRUE
        ELSE FALSE
    END,
    -- Fix incorrect FOUNDER assignments for employees
    role = CASE
        WHEN u.role = 'EMPLOYEE' AND csur.role = 'FOUNDER' THEN 'EMPLOYEE'
        WHEN u.role = 'OWNER' AND csur.role = 'FOUNDER' THEN 'FOUNDER'
        ELSE csur.role
    END
FROM users u
WHERE csur.user_id = u.id;

-- Step 4: Add foreign key constraints for the new columns
ALTER TABLE company_store_user_roles
ADD CONSTRAINT fk_csur_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

ALTER TABLE company_store_user_roles
ADD CONSTRAINT fk_csur_company
    FOREIGN KEY (company_id)
    REFERENCES companies(id)
    ON DELETE CASCADE;

ALTER TABLE company_store_user_roles
ADD CONSTRAINT fk_csur_store
    FOREIGN KEY (store_id)
    REFERENCES stores(id)
    ON DELETE CASCADE;

-- Step 5: Create indexes for performance on new columns
CREATE INDEX IF NOT EXISTS idx_csur_user_id ON company_store_user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_csur_company_id ON company_store_user_roles(company_id);
CREATE INDEX IF NOT EXISTS idx_csur_store_id ON company_store_user_roles(store_id);
CREATE INDEX IF NOT EXISTS idx_csur_user_company ON company_store_user_roles(user_id, company_id);
CREATE INDEX IF NOT EXISTS idx_csur_permanent ON company_store_user_roles(permanent);
CREATE INDEX IF NOT EXISTS idx_csur_expires_at ON company_store_user_roles(expires_at);

-- Step 6: Add comment explaining the enhancement
COMMENT ON TABLE company_store_user_roles IS 'Enhanced role mapping table with direct user/company/store FKs for simplified queries. Replaces user_store_roles table.';
COMMENT ON COLUMN company_store_user_roles.user_id IS 'Direct FK to users table for quick lookups without joining company_store_user';
COMMENT ON COLUMN company_store_user_roles.company_id IS 'Direct FK to companies table for filtering by company';
COMMENT ON COLUMN company_store_user_roles.store_id IS 'Direct FK to stores table (nullable for company-level roles)';
COMMENT ON COLUMN company_store_user_roles.expires_at IS 'Timestamp when role expires (null for permanent roles)';
COMMENT ON COLUMN company_store_user_roles.permanent IS 'TRUE for founders and permanent roles, FALSE for temporary assignments';
