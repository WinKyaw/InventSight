-- V12__create_refresh_tokens_table.sql
-- Create refresh_tokens table for JWT refresh token management

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    token VARCHAR(1000) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_token_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_token_revoked ON refresh_tokens(revoked);
CREATE INDEX idx_refresh_token_valid ON refresh_tokens(user_id, revoked, expires_at);

-- Add comments for documentation
COMMENT ON TABLE refresh_tokens IS 'Refresh tokens for JWT token refresh mechanism';
COMMENT ON COLUMN refresh_tokens.id IS 'Unique identifier for the refresh token';
COMMENT ON COLUMN refresh_tokens.user_id IS 'User who owns this refresh token';
COMMENT ON COLUMN refresh_tokens.token IS 'The actual refresh token string';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Expiration time (7 days from creation)';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Whether the token has been revoked';
COMMENT ON COLUMN refresh_tokens.ip_address IS 'IP address of the client when token was created';
COMMENT ON COLUMN refresh_tokens.user_agent IS 'User agent string of the client';
