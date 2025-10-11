-- Flyway migration to add system-wide audit events table
-- Append-only with optional hash chaining for tamper-evidence
-- PostgreSQL-compatible

CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(255) NOT NULL,
    actor_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    tenant_id UUID,
    company_id UUID,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    details_json TEXT,
    prev_hash VARCHAR(64),
    hash VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_audit_events_at ON audit_events(event_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events(actor);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor_id ON audit_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_action ON audit_events(action);
CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_tenant ON audit_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_company ON audit_events(company_id);

-- Add comment to make append-only nature explicit
COMMENT ON TABLE audit_events IS 'Append-only audit log. Records must not be updated or deleted.';
