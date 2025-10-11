-- Flyway migration to add idempotency tracking for offline sync
-- PostgreSQL-compatible

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL,
    tenant_id UUID,
    company_id UUID,
    endpoint VARCHAR(500) NOT NULL,
    request_hash VARCHAR(64),
    response_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_idempotency_key_tenant UNIQUE (idempotency_key, tenant_id)
);

-- Indexes for efficient lookup and cleanup
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_keys(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_tenant ON idempotency_keys(tenant_id);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_keys(expires_at);

-- Sync change log for change feed tracking
CREATE TABLE IF NOT EXISTS sync_changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    company_id UUID,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL, -- INSERT, UPDATE, DELETE
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_data TEXT,
    CONSTRAINT uq_sync_change UNIQUE (tenant_id, entity_type, entity_id, changed_at)
);

-- Indexes for change feed queries
CREATE INDEX IF NOT EXISTS idx_sync_changes_tenant_time ON sync_changes(tenant_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_changes_entity ON sync_changes(entity_type, entity_id);

COMMENT ON TABLE idempotency_keys IS 'Tracks idempotency keys for offline sync operations';
COMMENT ON TABLE sync_changes IS 'Change feed for offline sync reconciliation';
