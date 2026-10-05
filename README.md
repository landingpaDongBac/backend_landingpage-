# Agricultural Landing Page Backend

Standalone Spring Boot backend for customer consultation management, landing page CMS content, media management, authentication, Swagger API testing, Supabase PostgreSQL, and Render deployment.

This project intentionally does not modify or connect to the existing React frontend.

## Requirements

- Java 21
- Supabase PostgreSQL or another PostgreSQL database for local development
- Cloudinary credentials for media upload APIs

The included `mvnw.cmd` downloads a local Maven distribution when Maven is not installed.

## Local Setup

1. Copy `.env.example` to `.env` in the project root and set real values. The application loads this local file automatically; Git ignores it.
2. Create a PostgreSQL database.
3. Run:

```powershell
cd "C:\backend landing page"
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Health check:

```text
http://localhost:8080/actuator/health
```

## Initial Administrator

There is no public admin registration endpoint. On startup, if no users exist and both `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD` are set, the backend creates one `ADMIN` account with a BCrypt password hash. Remove those environment variables after the first successful bootstrap.

The bootstrap password must contain at least 12 characters. Additional `ADMIN` and `EDITOR` accounts are managed through the ADMIN-only `/api/admin/users` APIs. There is no public registration surface, and the final active ADMIN cannot be disabled or demoted.

## API Surface

Public endpoints:

- `POST /api/public/leads`
- `GET /api/public/content`
- `GET /api/public/content/{slug}`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /actuator/health`

Authenticated CMS endpoints are under `/api/admin/sections`, `/api/admin/content`, and `/api/admin/media`. Lead administration is under `/api/admin/leads` and is restricted to `ADMIN`. `EDITOR` can read and edit content drafts and manage media; section visibility, publish, unpublish, content archive, and all lead data require `ADMIN`.

Additional ADMIN-only modules are available at `/api/admin/dashboard/summary`, `/api/admin/settings`, `/api/admin/users`, `/api/admin/permissions`, and `/api/admin/audit-logs`. Global revision search is at `/api/admin/revisions`. The complete inventory and frontend integration contract are in [`docs/api-inventory.md`](docs/api-inventory.md) and [`docs/frontend-backend-contract.md`](docs/frontend-backend-contract.md).

Send access JWTs as `Authorization: Bearer <token>`. Login returns a single-use rotating refresh token; use the newest token at `/api/auth/refresh` and revoke it through `/api/auth/logout`. Swagger documents every DTO and endpoint at `/swagger-ui/index.html`.

## Rich-text Contract

`contentJson` is Tiptap/ProseMirror-compatible JSON with a root node of `{"type":"doc"}`. It supports headings H1-H6, paragraphs, standard text marks, lists, tables, blockquotes, code blocks, links, figures/captions, callouts, images, videos, and a generic `customBlock` node for controlled extensions.

Image and video nodes use either `image`/`video` or the Admin Editor's `mediaImage`/`mediaVideo` names. They must contain `attrs.mediaId` referencing a registered `MediaAsset`. URLs and formatting attributes may be retained, but script payloads, event-handler attributes, executable URL schemes, unsupported nodes/marks, oversized documents, and excessive nesting are rejected. Binary media must never be embedded as Base64.

Create requests may omit `version`. Every `PUT /api/admin/content/{id}` update must send the latest `version` returned by the API; stale versions receive HTTP `409` instead of overwriting another editor's work. Editing a published document changes only its draft JSON. Public APIs continue to serve `publishedContentJson` until an `ADMIN` explicitly publishes again.

## Media Uploads

- `POST /api/admin/media/upload` accepts authenticated multipart image/video uploads.
- `POST /api/admin/media/upload-signature` returns signed, folder-scoped upload parameters and allowed formats for future direct/chunked browser uploads.
- `POST /api/admin/media/confirm` verifies the uploaded Cloudinary resource before registering metadata.

After direct upload, submit the returned Cloudinary `public_id`, its `IMAGE` or `VIDEO` resource type, and optional original filename to the confirm endpoint. Media referenced by a draft, published snapshot, or retained revision cannot be deleted.

## Environment Variables

```env
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=2592000000
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
FRONTEND_URL=
INITIAL_ADMIN_EMAIL=
INITIAL_ADMIN_PASSWORD=
```

## Render Deployment

Create a Render Web Service from this repository, use the included `Dockerfile`, and configure all environment variables in Render. The application binds to `0.0.0.0` and uses Render's `PORT` environment variable.

Use Supabase as PostgreSQL through JDBC. If using a Supabase pooler, choose a JDBC URL and pool settings compatible with prepared statements and SSL requirements.

For a direct Supabase connection, use its JDBC URL with SSL enabled. For the Supabase transaction pooler, use the pooler host/port and keep the application pool small; append provider-recommended SSL and prepared-statement parameters to `DB_URL`. Render's production profile requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `FRONTEND_URL`, and all three Cloudinary credentials.

Recommended Render health-check path: `/actuator/health`.

## Verification

Run all automated checks with:

```powershell
.\mvnw.cmd clean verify
```

Unit tests cover JWT signing, refresh rotation/replay, public lead rules/rate limiting, rich-text validation including custom media nodes, draft publication behavior, optimistic version conflicts, dashboard queries, section ordering, last-admin safeguards, media validation, signed uploads, and referenced-media deletion protection. A PostgreSQL Testcontainers test validates Flyway and seeded sections when Docker is available.

## Notes

- Media is stored in Cloudinary, not PostgreSQL or Render disk.
- Rich text content is stored as Tiptap/ProseMirror-compatible JSONB.
- Sections 04 and 09 are seeded but disabled by default and are excluded from public content APIs.
- Automated unit tests mock external services. Real Supabase and Cloudinary integration tests require credentials and are not claimed as passing unless explicitly run in a configured environment.
