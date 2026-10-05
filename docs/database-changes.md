# Database Changes

Flyway history is preserved. `V1` creates the original schema and `V2` seeds roles and stable sections. `V3__complete_admin_backend.sql` is additive and safe for populated PostgreSQL databases.

## V3 additions

- `leads.deleted_at` while retaining `leads.deleted`.
- User profile/security fields: `display_name`, `last_login_at`, `failed_login_attempts`, `locked_until`, `password_changed_at`.
- Section `description` and JSONB `configuration_json`.
- Content `published_by` and `archived_at`.
- Revision `change_reason`.
- Media `mime_type`.
- `refresh_tokens`: hashed rotating tokens, family identifiers, expiry/revocation/replacement metadata, and user FK.
- `system_settings`: one typed singleton row when settings are first saved.
- `audit_logs`: actor snapshot, structured action/entity data, timestamp, and client IP.
- Supporting indexes for sessions, audit filters, and recent content/revision queries.

No existing table, column, index, row, or section key is dropped or renamed. Hibernate remains `ddl-auto: validate`; Flyway is the only schema writer.

## Applying

At application startup, Flyway applies pending migrations before Hibernate validation. For Supabase use the Session Pooler/direct JDBC endpoint on port 5432 with `sslmode=require`. Back up production data according to the normal Supabase policy before the first production rollout.

Verification SQL:

```sql
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name IN
  ('users', 'leads', 'sections', 'content_documents', 'content_revisions', 'media_assets')
ORDER BY table_name, ordinal_position;

SELECT section_key, id, display_order, is_enabled
FROM sections
ORDER BY display_order;
```

The expected stable section count is nine. `04` and `09` remain present and disabled unless an administrator explicitly changes visibility later.

On 2026-10-03, the configured Supabase database was an empty schema. Application startup successfully validated and applied V1, V2, and V3, after which Hibernate schema validation completed successfully. No pre-existing business rows were deleted or replaced.
