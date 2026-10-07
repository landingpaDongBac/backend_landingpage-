ALTER TABLE system_settings
    ADD COLUMN IF NOT EXISTS support_hours VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(500);
