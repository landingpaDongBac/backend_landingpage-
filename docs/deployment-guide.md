# Deployment Guide

## Required environment

```env
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:postgresql://HOST:5432/postgres?sslmode=require
DB_USERNAME=postgres.PROJECT_REF
DB_PASSWORD=...
JWT_SECRET=at-least-32-random-characters
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=2592000000
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
FRONTEND_URL=https://admin.example.com
INITIAL_ADMIN_EMAIL=admin@example.com
INITIAL_ADMIN_PASSWORD=strong-one-time-bootstrap-password
```

`FRONTEND_URL` may contain a comma-separated allowlist. Remove initial-admin variables after the first account is created. Never commit `.env`; Render values belong in the service environment.

## Supabase

Use the direct endpoint or Session Pooler on port 5432 for long-lived Spring/JPA connections. Keep the configured Hikari pool small. Require SSL. Do not use Hibernate schema creation and do not manually rerun Flyway SQL after Flyway has recorded it.

## Render

1. Create a Web Service from this backend repository and select the included Dockerfile.
2. Configure the environment variables above.
3. Set health-check path to `/actuator/health`.
4. Deploy. The image builds with Java 21 and runs the JRE image on Render's supplied `PORT`.
5. Confirm health, then `/v3/api-docs` and `/swagger-ui/index.html`.
6. Log in and verify `/api/auth/session`; inspect Flyway history before enabling frontend traffic.

The service logs to stdout/stderr and does not depend on persistent local disk. Production secrets are never exposed by settings APIs; only Boolean connectivity/configuration status is returned.

## Rollout notes

Migration V3 is additive. Still take the normal production backup before first rollout. Do not auto-deploy from a developer machine without authorization. Real Cloudinary deletion/upload and Supabase connectivity should be smoke-tested with production-like credentials after deployment.
