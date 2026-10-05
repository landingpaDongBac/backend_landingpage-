CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE roles (
    name VARCHAR(50) PRIMARY KEY
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL REFERENCES roles(name),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE leads (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    crop_type VARCHAR(255) NOT NULL,
    tree_count INTEGER,
    garden_area VARCHAR(255),
    consultation_message VARCHAR(2000),
    source VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    admin_notes TEXT,
    consent_accepted BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    contacted_at TIMESTAMPTZ,
    follow_up_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_leads_deleted ON leads(deleted);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_source ON leads(source);
CREATE INDEX idx_leads_created_at ON leads(created_at);
CREATE INDEX idx_leads_phone_number ON leads(phone_number);
CREATE INDEX idx_leads_crop_type ON leads(crop_type);

CREATE TABLE sections (
    id UUID PRIMARY KEY,
    section_key VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL,
    is_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE content_documents (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL REFERENCES sections(id),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content_json JSONB NOT NULL,
    published_content_json JSONB,
    status VARCHAR(50) NOT NULL,
    display_order INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    created_by VARCHAR(320),
    updated_by VARCHAR(320)
);

CREATE INDEX idx_content_documents_section ON content_documents(section_id);
CREATE INDEX idx_content_documents_status ON content_documents(status);
CREATE INDEX idx_content_documents_display_order ON content_documents(display_order);

CREATE TABLE content_revisions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES content_documents(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    content_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(320),
    CONSTRAINT uq_content_revision_version UNIQUE (document_id, version_number)
);

CREATE INDEX idx_content_revisions_document ON content_revisions(document_id);

CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    cloudinary_public_id VARCHAR(500) NOT NULL UNIQUE,
    cloudinary_asset_id VARCHAR(255),
    resource_type VARCHAR(50) NOT NULL,
    format VARCHAR(50),
    original_file_name VARCHAR(500),
    secure_url TEXT NOT NULL,
    file_size BIGINT,
    width INTEGER,
    height INTEGER,
    duration DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(320)
);

CREATE INDEX idx_media_assets_resource_type ON media_assets(resource_type);
CREATE INDEX idx_media_assets_created_at ON media_assets(created_at);
