-- User Active Store Tracking Table
CREATE TABLE IF NOT EXISTS user_active_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    store_id UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_active_store_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_active_store_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_active_store_user UNIQUE (user_id)
);

CREATE INDEX idx_user_active_store_user_id ON user_active_store(user_id);
CREATE INDEX idx_user_active_store_store_id ON user_active_store(store_id);

COMMENT ON TABLE user_active_store IS 'Tracks which store a user is currently active in';
COMMENT ON COLUMN user_active_store.user_id IS 'User ID';
COMMENT ON COLUMN user_active_store.store_id IS 'Currently active store ID';
COMMENT ON COLUMN user_active_store.updated_at IS 'Last time the active store was changed';
