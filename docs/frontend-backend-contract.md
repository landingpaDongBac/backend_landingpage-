# Frontend/Backend Contract

## Authentication flow

1. Send `POST /api/auth/login` with `{ "email": "...", "password": "..." }`.
2. Keep `accessToken` in memory and send it as a Bearer token. The response also contains `refreshToken`, `expiresInSeconds`, and `refreshExpiresInSeconds`.
3. On access-token expiry, call `POST /api/auth/refresh` with `{ "refreshToken": "..." }`. Replace both old tokens with the returned pair. A refresh token is single-use.
4. Call `POST /api/auth/logout` with the latest refresh token. Logout revokes the complete token family.
5. `GET /api/auth/session` or `/me` validates the access token and current account/session state.

Refresh tokens are opaque values. Do not decode them, log them, or persist them in ordinary application logs. The backend stores only SHA-256 hashes. Five consecutive failed logins lock an account for 15 minutes. Disabled/locked accounts cannot authenticate.

## Roles

- `ADMIN`: leads, dashboard, publish/archive, sections, media, users, settings, permissions, and audit logs.
- `EDITOR`: content drafts, revision read/restore, section read, and media management. EDITOR cannot read leads, manage users/settings, or publish.

## Canonical field names

Lead requests/responses use `consultationMessage` and `followUpAt`. For migration convenience, requests also accept `message` and `followUpDate`; responses remain canonical. `deleted` is retained and `deletedAt` is additive.

Media responses use `originalFileName`, `resourceType`, `fileSize`, `secureUrl`, `publicId`, `cloudinaryPublicId`, and `cloudinaryAssetId`. Frontend display names map as follows:

| Frontend | Canonical backend |
|---|---|
| `filename` | `originalFileName` |
| `type` | `resourceType` (`IMAGE` or `VIDEO`) |
| `size` | `fileSize` |
| `cloudinaryUrl` | `secureUrl` |
| `publicId` | `publicId`/`cloudinaryPublicId` |

Section `id` is a PostgreSQL UUID. `sectionKey` is the stable business number (`02` through `10`). Never substitute one for the other. Section keys cannot be created through the API. Keys `04` and `09` remain seeded and disabled by default.

## Primary request contracts

Public lead:

```json
{
  "fullName": "Nguyen Van A",
  "phoneNumber": "0901234567",
  "cropType": "Sau rieng",
  "treeCount": 200,
  "gardenArea": "2 ha",
  "consultationMessage": "Can tu van",
  "source": "CONSULTATION_FORM",
  "consentAccepted": true,
  "website": ""
}
```

Content create/update:

```json
{
  "sectionId": "10000000-0000-0000-0000-000000000005",
  "title": "Loi ich san pham",
  "slug": "loi-ich-san-pham",
  "contentJson": { "type": "doc", "content": [] },
  "displayOrder": 1,
  "version": 3
}
```

`version` may be omitted on create and is required on update. Publishing may send the last seen numeric document version in `If-Match`. A stale value receives HTTP 409 and `CONTENT_VERSION_CONFLICT`.

Section order request:

```json
{
  "items": [
    { "sectionId": "UUID_FOR_02", "displayOrder": 2 },
    { "sectionId": "UUID_FOR_03", "displayOrder": 3 }
  ]
}
```

The request must contain every managed section exactly once with unique non-negative orders.

## Enums

- Role: `ADMIN`, `EDITOR`.
- Lead status: `NEW`, `CONTACTED`, `QUALIFIED`, `CONVERTED`, `CLOSED`.
- Lead source: `PROMOTION_FORM`, `CONSULTATION_FORM`, `PRIORITY_BOOKING`, `REQUEST_QUOTATION`, `OTHER`.
- Content status: `DRAFT`, `PUBLISHED`, `ARCHIVED`.
- Media resource type: `IMAGE`, `VIDEO`.

Enum JSON values are never translated. Vietnamese labels belong in the frontend.

## Pagination and sorting

Paged responses use Spring's stable DTO shape and include `content`, `page`, `size`, `totalElements`, and `totalPages`. Page numbers are zero-based; maximum size is 100. Date query values are ISO 8601 instants. Sort direction is `ASC` or `DESC`.

The compatibility endpoint `GET /api/admin/content` intentionally remains an unpaged array. New integration should use `/api/admin/content/page`.

## Errors

Errors are not wrapped in a success envelope, preserving the existing API. The additive response is:

```json
{
  "timestamp": "2026-10-03T14:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "CONTENT_VERSION_CONFLICT",
  "message": "Content was changed by another user; reload before saving",
  "path": "/api/admin/content/...",
  "fieldErrors": {}
}
```

Important codes include `AUTHENTICATION_REQUIRED`, `AUTHENTICATION_FAILED`, `PERMISSION_DENIED`, `VALIDATION_FAILED`, `CONTENT_VERSION_CONFLICT`, `CONTENT_VALIDATION_FAILED`, `SECTION_DISABLED`, `MEDIA_NOT_READY`, `LAST_ACTIVE_ADMIN`, and `RATE_LIMIT_EXCEEDED`.

## Tiptap JSON

- Root must be `{ "type": "doc" }`; JSON is stored as PostgreSQL JSONB without flattening.
- Supported media nodes are both legacy `image`/`video` and frontend `mediaImage`/`mediaVideo`.
- Every media node requires `attrs.mediaId` containing a registered media UUID of the matching type.
- Arbitrary placement among headings, paragraphs, lists, tables, figures, captions, and callouts is preserved.
- Supported marks include bold, italic, underline, strike, text style/font size, highlight, links, subscript, superscript, and code.
- Browser object URLs and external media-only `src` values are not accepted as durable media references.
- Script/event-handler payloads, executable URLs, excessive nesting, oversized documents, and unsupported node/mark types are rejected.

Draft updates never replace `publishedContentJson`. Only ADMIN publish copies the current draft into the public snapshot. Restoring a revision copies it into the draft and creates a new revision; existing history remains immutable.

Public content responses also include a `media` array containing the minimal trusted rendering metadata (`id`, `resourceType`, `mimeType`, `secureUrl`, dimensions, and duration) for every media UUID used by that published snapshot. They do not expose drafts, Cloudinary credentials, uploader identity, or other administrative metadata.

## Cloudinary direct upload

Request a signature, upload directly using every returned signed parameter, then confirm using `{ "publicId": "...", "resourceType": "IMAGE|VIDEO", "originalFileName": "..." }`. Treat confirmation as the point at which the asset becomes selectable by content. See `cloudinary-upload-flow.md`.
