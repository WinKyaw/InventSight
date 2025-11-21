-- V11__create_one_time_permissions_table.sql
-- Create one_time_permissions table for temporary permission grants

CREATE TABLE IF NOT EXISTS one_time_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    granted_to_user_id BIGINT NOT NULL,
    granted_by_user_id BIGINT NOT NULL,
    permission_type VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    is_expired BOOLEAN NOT NULL DEFAULT FALSE,
    store_id UUID,
    CONSTRAINT fk_otp_granted_to_user FOREIGN KEY (granted_to_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_otp_granted_by_user FOREIGN KEY (granted_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_otp_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT chk_permission_type CHECK (permission_type IN ('ADD_ITEM', 'EDIT_ITEM', 'DELETE_ITEM'))
);

-- Create indexes for better query performance
CREATE INDEX idx_otp_granted_to_user ON one_time_permissions(granted_to_user_id);
CREATE INDEX idx_otp_granted_by_user ON one_time_permissions(granted_by_user_id);
CREATE INDEX idx_otp_permission_type ON one_time_permissions(permission_type);
CREATE INDEX idx_otp_is_used ON one_time_permissions(is_used);
CREATE INDEX idx_otp_is_expired ON one_time_permissions(is_expired);
CREATE INDEX idx_otp_expires_at ON one_time_permissions(expires_at);
CREATE INDEX idx_otp_store_id ON one_time_permissions(store_id);
CREATE INDEX idx_otp_active_permissions ON one_time_permissions(granted_to_user_id, permission_type, is_used, is_expired);

-- Add comments for documentation
COMMENT ON TABLE one_time_permissions IS 'Temporary permissions granted to employees for specific actions';
COMMENT ON COLUMN one_time_permissions.id IS 'Unique identifier for the permission grant';
COMMENT ON COLUMN one_time_permissions.granted_to_user_id IS 'User receiving the permission';
COMMENT ON COLUMN one_time_permissions.granted_by_user_id IS 'User granting the permission (must be GM+)';
COMMENT ON COLUMN one_time_permissions.permission_type IS 'Type of permission: ADD_ITEM, EDIT_ITEM, or DELETE_ITEM';
COMMENT ON COLUMN one_time_permissions.expires_at IS 'Expiration time (1 hour from grant)';
COMMENT ON COLUMN one_time_permissions.is_used IS 'Whether the permission has been consumed';
COMMENT ON COLUMN one_time_permissions.is_expired IS 'Whether the permission has expired';
