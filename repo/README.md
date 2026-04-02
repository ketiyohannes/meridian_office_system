# Meridian Offline Commerce Intelligence Portal - Dev Environment

This repository is prepared with a Dockerized local development environment.

## Services
- `mysql` (MySQL 8.4): local data persistence
- `app-dev` (Maven + JDK 21): Spring Boot development container

## First Run
1. Start containers:
   ```bash
   docker compose up
   ```
2. Check status:
   ```bash
   docker compose ps
   ```
3. Stop when needed (from the running terminal):
   - Press `Ctrl+C`
4. Or stop from another terminal:
   ```bash
   docker compose down
   ```

On startup, Compose runs `scripts/bootstrap_runtime.sh` automatically to:
- create `.env` from `.env.example` when missing,
- preserve existing `.env` values when present,
- create runtime config snapshots under `docker/runtime/`.

Optional: edit `.env` if you want to override defaults (ports, DB credentials, bootstrap secrets).

## Access URL
- Application root: `http://localhost:18080/`
- Login page: `http://localhost:18080/login`

## Default Admin Login
- Username: `admin`
- Password: `LocalDevAdmin!2026`

If you changed `APP_HOST_PORT` in `.env`, replace `18080` with your configured value.

## Run Tests In Docker
Run the full backend test suite in containers with one command:
```bash
./run_test.sh
```

`run_test.sh` always runs `scripts/bootstrap_runtime.sh` first, so `.env` and runtime configs are prepared automatically.

## Ports
- MySQL host port: `${MYSQL_HOST_PORT}` (default `3307`) -> container `3306`
- App host port: `${APP_HOST_PORT}` (default `18080`) -> container `8080`

## Notes
- Backend source should be built inside `./backend`.
- MySQL data persists in `./.mysql-data`.
- Seed/init SQL files go in `./docker/mysql/init`.

## Operations Runbook (Phase 1)
- Health screen: `/admin/health` (Admin only)
  - Tracks 5xx rate over sliding window (default 15 min, threshold 1%).
  - Stores structured error logs and alert history in MySQL.
- KPI dashboard: `/analytics` (Admin, Analyst, Ops Manager)
  - Supports filters (`from`, `to`, `category`, `channel`, `role`, `region`).
  - Exports CSV/XLSX from `/api/analytics/export.csv` and `/api/analytics/export.xlsx`.
  - Nightly report package job writes to `${MERIDIAN_REPORTING_SHARED_FOLDER}`.
  - Report package filename contract for listing/download:
    - `kpi-package-YYYYMMDD.csv`
    - `kpi-package-YYYYMMDD.xlsx`

## Production Hardening and Ops Packaging
- Production compose stack: `docker-compose.prod.yml`
- Runtime Docker image build: `backend/Dockerfile`
- Release packaging/checklist: `docs/ops/release_checklist.md`
- Rollback runbook: `docs/ops/rollback_runbook.md`
- Health SLO runbook: `docs/ops/health_slo_runbook.md`
- Offline dependency governance: `docs/ops/offline_dependency_governance.md`
- Key management policy: `docs/security/key_management_policy.md`
- UAT sign-off evidence: `docs/uat/uat_signoff_evidence.md`

## Backup and Restore
1. Create backup:
   ```bash
   ./scripts/backup_db.sh
   ```
2. Restore backup:
   ```bash
   ./scripts/restore_db.sh ./backups/<backup-file>.sql.gz
   ```

Environment variables supported by scripts:
- `SPRING_DATASOURCE_HOST` (default `localhost`)
- `SPRING_DATASOURCE_PORT` (default `3307`)
- `SPRING_DATASOURCE_DB` (default `meridian`)
- `SPRING_DATASOURCE_USERNAME` (default `meridian_user`)
- `SPRING_DATASOURCE_PASSWORD` (default `meridian_password`)
