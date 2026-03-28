# Meridian Rollback Runbook

## Preconditions
1. Previous stable image tag is known.
2. `.env` is present and valid.

## Rollback Steps
1. Execute rollback script:
   - `./scripts/release_rollback.sh <previous-version-tag>`
2. Verify service state:
   - `docker compose -f docker-compose.prod.yml ps`
3. Validate application endpoints:
   - `/api/auth/me`
   - `/api/admin/health/summary`
4. Confirm no new critical health alerts for 15 minutes.

## If rollback fails
1. Restore DB from latest backup:
   - `./scripts/restore_db.sh ./backups/<backup-file>.sql.gz`
2. Restart stack:
   - `docker compose -f docker-compose.prod.yml up -d`
3. Escalate incident and freeze further deploys.
