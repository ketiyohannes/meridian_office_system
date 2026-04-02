# Meridian Backend

Spring Boot + Thymeleaf + Spring Security backend for Meridian.

## Local non-Docker DB port convention

- Canonical local (non-Docker) MySQL port: `3306`
- If using repo Docker compose MySQL: host port `3307` mapped to container `3306`
- `SPRING_DATASOURCE_URL` should explicitly match your chosen mode to avoid ambiguity.

## Run in Docker dev container

1. Ensure root services are up:
   - `docker compose up -d`
2. Build:
   - `docker compose exec app-dev mvn -q -DskipTests package`
3. Run:
   - `docker compose exec app-dev mvn spring-boot:run`
4. Test:
   - `docker compose exec app-dev mvn test`

## Bootstrap admin credentials

Set via env vars (required):
- `MERIDIAN_BOOTSTRAP_ADMIN_USERNAME`
- `MERIDIAN_BOOTSTRAP_ADMIN_PASSWORD`
- `MERIDIAN_DATA_ENCRYPTION_KEY` (exactly 32 characters)

Password policy is enforced at bootstrap (minimum 12 characters).

## Admin user-management

UI:
- `/admin/users` (create users, list users)
- `/admin/users/{id}` (assign roles, reset password, enable/disable)
- `/admin/audit-logs` (recent admin audit trail)

API:
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}/roles`
- `PUT /api/admin/users/{id}/password`
- `PUT /api/admin/users/{id}/enabled`
- `GET /api/admin/audit-logs`
  - Supports query params: `actor`, `action`, `targetUsername`, `targetType`, `from`, `to`, `page`, `size`

## Discovery

UI:
- `/discovery`

API:
- `GET /api/discovery/search`
- `GET /api/discovery/suggestions`
- `GET /api/discovery/trending`
- `GET /api/discovery/history`
- `DELETE /api/discovery/history`

## Integration test suites

- `AuthIntegrationTest`
- `AdminUserManagementIntegrationTest`
- `AuditLogIntegrationTest`
- `DiscoveryIntegrationTest`
- `SecurityHardeningIntegrationTest`

## Analytics report package contract

- `GET /api/analytics/report-packages`
  - Returns only files matching strict naming:
    - `kpi-package-YYYYMMDD.csv`
    - `kpi-package-YYYYMMDD.xlsx`
- `GET /api/analytics/report-packages/download?file=...`
  - `file` must match the same strict naming contract.
