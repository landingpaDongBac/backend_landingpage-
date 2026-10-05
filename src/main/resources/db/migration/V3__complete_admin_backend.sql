ALTER TABLE leads ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;

ALTER TABLE sections ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
ALTER TABLE sections ADD COLUMN IF NOT EXISTS configuration_json JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE content_documents ADD COLUMN IF NOT EXISTS published_by VARCHAR(320);
ALTER TABLE content_documents ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;

ALTER TABLE content_revisions ADD COLUMN IF NOT EXISTS change_reason VARCHAR(500);

ALTER TABLE media_assets ADD COLUMN IF NOT EXISTS mime_type VARCHAR(150);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at);

CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY,
    website_name VARCHAR(255),
    default_language VARCHAR(20) NOT NULL DEFAULT 'vi',
    public_information TEXT,
    support_phone VARCHAR(50),
    contact_email VARCHAR(320),
    address VARCHAR(1000),
    default_lead_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    default_lead_source VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    publication_approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    preview_width INTEGER NOT NULL DEFAULT 1200,
    display_configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(320)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email VARCHAR(320),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    description VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    ip_address VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_content_documents_updated_at ON content_documents(updated_at);
CREATE INDEX IF NOT EXISTS idx_content_revisions_created_at ON content_revisions(created_at);
