-- Migration V21: Deprecate role column in company_store_user table
-- Mark the role column as deprecated - kept for backward compatibility
-- Applications should now use company_store_user_roles table instead

-- Add comment marking the column as deprecated
COMMENT ON COLUMN company_store_user.role IS 'DEPRECATED: Use company_store_user_roles table instead. This column is kept for backward compatibility only and may be removed in a future version.';

-- Add table-level comment explaining the deprecation
COMMENT ON TABLE company_store_user IS 'Company-User-Store membership table. Note: The role column is deprecated. Use company_store_user_roles table for role assignments.';
