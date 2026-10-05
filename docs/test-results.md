# Test Results

Last local run: 2026-10-03, Java 21, Windows.

Command:

```powershell
.\mvnw.cmd test
```

Final command: `.\mvnw.cmd clean verify`.

Result: 32 tests discovered, 31 passed, 0 failed, 0 errors, 1 skipped. The skipped test is `PostgresMigrationIntegrationTest`; Testcontainers correctly skipped it because no Docker daemon was available. The executable Spring Boot JAR was produced successfully.

Verified automated areas include:

- Bearer JWT creation/validation and ADMIN/EDITOR route authorization.
- Public lead validation, honeypot, rate limiting, normalization, and contact timestamp preservation.
- Draft creation, revisions, optimistic update conflict, snapshot publishing, and disabled-section rejection.
- Tiptap safety plus `mediaImage`/`mediaVideo` extraction.
- Multipart media signature checks, signed upload parameters, and referenced-media deletion protection.
- Refresh-token hashing, rotation, and replay-family revocation.
- Dashboard aggregation queries, duplicate section-order rejection, and last-active-ADMIN protection.
- Flyway/section seed test is present but needs Docker to execute.

Not claimed as locally verified:

- Live Cloudinary upload, confirmation, and deletion.
- Render deployment/runtime health.

Additional live smoke verification used the configured Supabase database: Flyway applied V1-V3 to the empty schema, Hibernate validation succeeded, Actuator returned `UP`, Swagger UI returned HTTP 200, and OpenAPI contained the new dashboard and refresh routes. Login, authenticated session lookup, dashboard read, refresh-token rotation, and logout all succeeded. The verification server was then stopped.
