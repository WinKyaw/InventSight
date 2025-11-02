-- Add default_tenant_id column to users table for automatic tenant binding
-- This enables users to have a default company/tenant that is automatically selected at login

-- Add default_tenant_id column (nullable)
ALTER TABLE public.users 
ADD COLUMN IF NOT EXISTS default_tenant_id UUID NULL;

-- Add foreign key constraint to companies table if companies table exists
-- This ensures referential integrity
ALTER TABLE public.users
ADD CONSTRAINT fk_users_default_tenant_id 
FOREIGN KEY (default_tenant_id) 
REFERENCES public.companies(id) 
ON DELETE SET NULL;

-- Add index on default_tenant_id for better query performance
CREATE INDEX IF NOT EXISTS idx_users_default_tenant_id 
ON public.users(default_tenant_id);

-- Add comment to document the column purpose
COMMENT ON COLUMN public.users.default_tenant_id IS 
'Default company/tenant ID for automatic tenant binding at login. Set during registration or invite acceptance.';
