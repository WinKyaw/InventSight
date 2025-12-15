-- Create user_navigation_preferences table for role-based navigation tab access control
-- Prevents 403 errors by storing only tabs the user has permission to access

CREATE TABLE IF NOT EXISTS user_navigation_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preferred_tabs JSONB DEFAULT '[]'::jsonb,
    available_tabs JSONB DEFAULT '[]'::jsonb,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster user lookups
CREATE INDEX IF NOT EXISTS idx_user_navigation_preferences_user_id ON user_navigation_preferences(user_id);

-- Add comment for documentation
COMMENT ON TABLE user_navigation_preferences IS 'Stores user navigation tab preferences with role-based access control';
COMMENT ON COLUMN user_navigation_preferences.preferred_tabs IS 'Tabs the user has chosen to display';
COMMENT ON COLUMN user_navigation_preferences.available_tabs IS 'Tabs the user has permission to access based on role';
