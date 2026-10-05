# API Inventory

Base URL examples: `http://localhost:8080` locally and the Render service URL in production. Protected requests use `Authorization: Bearer <accessToken>`.

## Authentication

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Authenticate and issue access/refresh tokens |
| POST | `/api/auth/refresh` | Public with refresh token | Rotate refresh token and issue a new token pair |
| POST | `/api/auth/logout` | Public with refresh token | Revoke the complete refresh-token family |
| GET | `/api/auth/me` | Authenticated | Current account |
| GET | `/api/auth/session` | Authenticated | Validate session and return current account |
| PATCH | `/api/auth/change-password` | Authenticated | Change own password and revoke all refresh sessions |

## Public

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/public/leads` | Submit a consented consultation request |
| GET | `/api/public/content` | Ordered published snapshots in enabled sections |
| GET | `/api/public/content/{slug}` | Published snapshot by slug |

## Dashboard and leads

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/admin/dashboard/summary` | ADMIN | Aggregated counts and recent activity |
| GET | `/api/admin/leads` | ADMIN | Search/filter/page leads |
| GET | `/api/admin/leads/{id}` | ADMIN | Lead detail |
| POST | `/api/admin/leads` | ADMIN | Create lead |
| PUT | `/api/admin/leads/{id}` | ADMIN | Replace editable lead fields |
| PATCH | `/api/admin/leads/{id}/status` | ADMIN | Status, notes, contacted/follow-up timestamps |
| DELETE | `/api/admin/leads/{id}` | ADMIN | Soft delete |
| PATCH | `/api/admin/leads/{id}/restore` | ADMIN | Restore soft-deleted lead |

Lead-list query: `q`, `crop`, `status`, `source`, `deleted`, `createdFrom`, `createdTo`, `page`, `size`, `sortBy`, `direction`. `sortBy` is whitelisted; unsupported values fall back to `createdAt`.

Dashboard `pendingContact` means an undeleted NEW or QUALIFIED lead with no contact timestamp, plus any undeleted non-CONVERTED/non-CLOSED lead whose follow-up time is due. The count is calculated in PostgreSQL and is not derived from an in-memory table scan.

## Sections and content

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/admin/sections` | ADMIN, EDITOR | Sections with document count/configuration |
| GET | `/api/admin/sections/{id}` | ADMIN, EDITOR | Section detail |
| PATCH | `/api/admin/sections/{id}/visibility` | ADMIN | Enable/disable section |
| PATCH | `/api/admin/sections/order` | ADMIN | Transactional full section reorder |
| PATCH | `/api/admin/sections/{id}` | ADMIN | Update allowed name/description/configuration |
| GET | `/api/admin/content` | ADMIN, EDITOR | Compatibility list ordered by section/document order |
| GET | `/api/admin/content/page` | ADMIN, EDITOR | Search/filter/page content |
| GET | `/api/admin/content/{id}` | ADMIN, EDITOR | Content detail |
| POST | `/api/admin/content` | ADMIN, EDITOR | Create draft and initial revision |
| PUT | `/api/admin/content/{id}` | ADMIN, EDITOR | Save draft with required current `version` |
| PATCH | `/api/admin/content/{id}/order` | ADMIN, EDITOR | Update document display order |
| PATCH | `/api/admin/content/{id}/publish` | ADMIN | Publish draft snapshot; optional numeric `If-Match` version header |
| PATCH | `/api/admin/content/{id}/unpublish` | ADMIN | Remove document from public delivery |
| DELETE | `/api/admin/content/{id}` | ADMIN | Archive, never physically delete |
| POST | `/api/admin/content/{id}/restore` | ADMIN | Restore archived document as draft |
| GET | `/api/admin/content/{id}/revisions` | ADMIN, EDITOR | Document revision history |
| GET | `/api/admin/content/{id}/revisions/{revisionId}` | ADMIN, EDITOR | Revision detail |
| POST | `/api/admin/content/{id}/revisions/{revisionId}/restore` | ADMIN, EDITOR | Copy old JSON into a new current revision |
| GET | `/api/admin/revisions` | ADMIN, EDITOR | Global revision search/filter/page |

Content-page query: `q`, `sectionId`, `status`, `page`, `size`, `sortBy`, `direction`. Global revision query: `q`, `documentId`, `createdBy`, `changeReason`, `createdFrom`, `createdTo`, `page`, `size`.

## Media

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/admin/media` | ADMIN, EDITOR | Search/filter/sort/page assets |
| GET | `/api/admin/media/{id}` | ADMIN, EDITOR | Asset detail |
| GET | `/api/admin/media/{id}/usages` | ADMIN, EDITOR | Draft, published and revision references |
| POST | `/api/admin/media/upload` | ADMIN, EDITOR | Server-side multipart upload |
| POST | `/api/admin/media/upload-signature` | ADMIN, EDITOR | Short-lived direct-upload signature |
| POST | `/api/admin/media/confirm` | ADMIN, EDITOR | Verify Cloudinary resource and persist metadata |
| DELETE | `/api/admin/media/{id}` | ADMIN, EDITOR | Delete only when unreferenced |

Media-list query: `q`, `resourceType`, `page`, `size`, `sortBy`, `direction`.

## Administration

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/admin/settings` | ADMIN | Typed settings plus safe infrastructure status |
| PATCH | `/api/admin/settings` | ADMIN | Partial settings update |
| GET | `/api/admin/users` | ADMIN | Search/filter/page users |
| GET | `/api/admin/users/{id}` | ADMIN | User detail |
| POST | `/api/admin/users` | ADMIN | Create ADMIN/EDITOR account |
| PUT | `/api/admin/users/{id}` | ADMIN | Update email/display name |
| PATCH | `/api/admin/users/{id}/status` | ADMIN | Activate/deactivate/unlock |
| PATCH | `/api/admin/users/{id}/roles` | ADMIN | Replace roles and revoke sessions |
| POST | `/api/admin/users/{id}/reset-password` | ADMIN | Set supplied strong password and revoke sessions |
| GET | `/api/admin/permissions` | ADMIN | Role permission inventory |
| GET | `/api/admin/audit-logs` | ADMIN | Filter/page administrative audit records |

## Operations

| Method | Path | Access |
|---|---|---|
| GET | `/actuator/health` | Public |
| GET | `/v3/api-docs` | Public |
| GET | `/swagger-ui/index.html` | Public |
