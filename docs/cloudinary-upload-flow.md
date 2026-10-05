# Cloudinary Upload Flow

## Server-side multipart upload

Use `POST /api/admin/media/upload` as multipart/form-data with part `file` and query parameter `resourceType=IMAGE|VIDEO`. The backend validates extension and magic bytes, uploads to the configured folder with overwrite disabled, and stores only trusted Cloudinary metadata in PostgreSQL.

Allowed image formats: JPG, JPEG, PNG, WEBP. Allowed video formats: MP4, MOV, WEBM.

## Signed direct upload

1. Call `POST /api/admin/media/upload-signature` with `{ "resourceType": "VIDEO" }` or `IMAGE`.
2. Use the returned `cloudName`, `apiKey`, `timestamp`, `signature`, `folder`, `resourceType`, `allowedFormats`, `useFilename`, `uniqueFilename`, and `overwrite` exactly in the Cloudinary upload request.
3. Upload the binary directly to Cloudinary. The API secret never goes to the browser.
4. Call `POST /api/admin/media/confirm` with the returned Cloudinary `public_id`, matching backend resource type, and optional original filename.
5. The backend requires the configured folder, fetches the resource through the authenticated Cloudinary Admin API, and persists trusted metadata. Repeating confirmation for the same public ID returns the existing record.

If Cloudinary succeeds but confirmation fails, retain the returned `public_id` and retry confirmation. Do not insert media records directly in Supabase.

## Usage and deletion

Call `GET /api/admin/media/{id}/usages` before presenting destructive UI. The response identifies draft, published snapshot, and retained revision references. `DELETE /api/admin/media/{id}` independently repeats this check and returns HTTP 409 while any reference exists.

The database stores URLs and metadata, never original binary data. Render local disk and Supabase Storage are not used.
