# Backend Gap Audit

Audit date: 2026-10-03

## Scope and method

The implementation under `src/main`, Flyway migrations, configuration, Dockerfile, README, and automated tests were inspected directly. The documented Admin Frontend path `C:\admin landing page` was not present on this machine, so frontend-specific findings are based on the contract supplied in the task. No frontend files are changed by this work.

## Existing API inventory

### Existing and working

- Authentication: `POST /api/auth/login`, `GET /api/auth/me`.
- Public leads: `POST /api/public/leads`, including validation, consent, honeypot, and in-memory per-IP rate limiting.
- Lead administration: list/search/filter/page, detail, create, update, status update, soft delete, and restore under `/api/admin/leads`.
- Sections: list, detail, and ADMIN-only visibility update under `/api/admin/sections`.
- CMS: list, detail, create, update, order, publish, unpublish, archive, document revisions, and revision restore under `/api/admin/content`.
- Public CMS: `GET /api/public/content` and `GET /api/public/content/{slug}` expose only published snapshots in enabled sections.
- Media: list, detail, multipart upload, signed direct-upload parameters, verified confirmation, and guarded deletion under `/api/admin/media`.
- Operations: Actuator health, OpenAPI JSON, and Swagger UI.

### Existing but incomplete

- JWT authentication had no refresh-token rotation, revocation, logout, password change, login tracking, or lockout lifecycle.
- Lead deletion used only a Boolean marker and had no deletion timestamp or dedicated crop filter.
- Section responses had no document counts/configuration; section ordering and metadata update were missing.
- Content admin listing was unpaged and unfiltered; archived restore, publisher/archive metadata, global revision search, and change reasons were missing.
- Publishing did not reject disabled sections or empty documents and did not accept an optional optimistic version precondition.
- Rich-text validation accepted `image`/`video` but not the frontend's `mediaImage`/`mediaVideo` node names.
- Media metadata lacked MIME type, list sorting was fixed, and no usage-detail endpoint existed.
- Error payloads were consistent structurally but lacked stable machine-readable codes.
- Audit coverage was limited to ordinary application logs.

### Missing

- `GET /api/admin/dashboard/summary`.
- Persistent settings APIs.
- Administrative user/role/password/status management and permission inventory.
- Persistent audit log and query API.
- Global revision list.
- Refresh-token/session persistence.

### Requires contract alignment

- Canonical lead fields remain `consultationMessage` and `followUpAt`; frontend names `message` and `followUpDate` are aliases documented for the integration phase, not duplicate database columns.
- Canonical media fields remain `originalFileName`, `resourceType`, `fileSize`, `secureUrl`, and `publicId`; frontend display aliases are documented separately.
- Section business keys (`02` through `10`) remain distinct from UUID primary keys. Keys `04` and `09` remain reserved and disabled by default.
- Existing unwrapped success responses are preserved. Adding a blanket success envelope would break existing consumers.

### Blocked by external configuration

- Real Supabase migration/startup verification requires working `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Real Cloudinary upload/delete verification requires Cloudinary credentials.
- Docker/Testcontainers verification requires a running Docker daemon.
- Render runtime verification requires an authorized Render service and production environment variables.

## Existing database entities

- `User` plus `user_roles` and seeded `ADMIN`/`EDITOR` roles.
- `Lead`.
- `Section` with stable seeded UUIDs and business keys 02-10.
- `ContentDocument` with JSONB draft and published snapshots and JPA optimistic versioning.
- `ContentRevision` with per-document revision numbers.
- `MediaAsset` containing Cloudinary metadata only.

## Existing security rules

- Stateless Spring Security with Bearer JWT and BCrypt cost 12.
- Public access is restricted to public lead/content, login, health, and API documentation.
- Lead administration is ADMIN-only.
- Draft CMS and media are ADMIN/EDITOR; visibility, publish, unpublish, and archive are ADMIN-only.
- Disabled users are rejected by `UserDetails`.

## Database migrations required

One additive migration is required for deletion timestamps, account/session metadata, content publication metadata, revision reasons, media MIME type, section configuration, refresh tokens, settings, and audit logs. Existing tables, columns, migration history, and section identifiers must remain intact.

## Existing Cloudinary capabilities

- Multipart image/video upload with extension and magic-byte checks.
- Folder-scoped signed upload parameters with restricted formats and overwrite disabled.
- Confirmation fetches trusted metadata from Cloudinary and is idempotent by public ID.
- Deletion checks draft, published snapshot, and retained revision references.

## Test coverage gaps before implementation

- Refresh rotation/replay/logout/change-password and account lock behavior.
- Dashboard aggregation.
- Section batch ordering and content counts.
- `mediaImage`/`mediaVideo` compatibility.
- Archive restore, global revisions, and disabled-section publishing.
- Settings, user management safeguards, media usages, audit querying, and coded errors.
- Live Supabase, Cloudinary, and Render checks remain external integration work.

## Implementation priorities

1. Add the safe Flyway migration and align Tiptap/lead/content/media contracts.
2. Complete authentication sessions and user controls.
3. Add dashboard, section ordering/configuration, content search/restore/global revisions, and media usages.
4. Add typed settings and audit logging.
5. Expand automated tests, OpenAPI-facing documentation, deployment documentation, and run `clean verify`.
