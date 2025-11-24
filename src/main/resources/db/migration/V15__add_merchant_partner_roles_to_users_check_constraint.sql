-- Migration V15: Add MERCHANT and PARTNER roles to users role check constraint
-- This allows users to be assigned the new MERCHANT and PARTNER roles

-- Drop the existing check constraint if it exists
-- Note: Constraint name may vary based on database generation
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Add new constraint with all roles including MERCHANT and PARTNER
ALTER TABLE users ADD CONSTRAINT users_role_check 
CHECK (role IN ('OWNER', 'CO_OWNER', 'MANAGER', 'EMPLOYEE', 'CUSTOMER', 'MERCHANT', 'PARTNER', 'USER', 'ADMIN', 'CASHIER'));
