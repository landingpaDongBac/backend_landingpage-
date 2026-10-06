ALTER TABLE system_settings
    ADD COLUMN IF NOT EXISTS zalo_url VARCHAR(500);

UPDATE system_settings
SET zalo_url = CASE
    WHEN jsonb_typeof(display_configuration -> 'zalo') = 'string'
        THEN display_configuration ->> 'zalo'
    WHEN jsonb_typeof(display_configuration -> 'zalo') = 'object'
        THEN display_configuration #>> '{zalo,url}'
    WHEN jsonb_typeof(display_configuration #> '{contact,zalo}') = 'string'
        THEN display_configuration #>> '{contact,zalo}'
    ELSE NULL
END
WHERE zalo_url IS NULL;
