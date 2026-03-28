# Meridian Release Checklist

## Pre-release
1. Run full tests:
   - `cd backend && mvn -q test`
2. Verify no default production secrets in target `.env`:
   - `MERIDIAN_BOOTSTRAP_ADMIN_PASSWORD`
   - `MERIDIAN_DATA_ENCRYPTION_KEY`
3. Build release artifact:
   - `./scripts/release_build.sh <version>`

## Deployment
1. Set image variables:
   - `export MERIDIAN_APP_IMAGE=meridian/portal`
   - `export MERIDIAN_APP_VERSION=<version>`
2. Start production stack:
   - `docker compose -f docker-compose.prod.yml up -d`
3. Validate health:
   - `GET /api/auth/me` (authenticated)
   - `GET /api/admin/health/summary` (admin)

## Post-deployment smoke
1. Admin login and user management page works.
2. Discovery search returns results.
3. Notification inbox loads and mark-read works.
4. Analytics dashboard and exports work.
5. Recommendations render on Home and Discovery.

## Rollback trigger
1. Health alert sustained above SLO threshold.
2. Authentication or RBAC regression.
3. Data integrity regression in critical workflows.
