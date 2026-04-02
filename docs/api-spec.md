# Meridian Portal API Spec

## 1. Scope
This document describes the current REST API implemented by the Meridian Offline Commerce Intelligence Portal.

- Base path: `/api/**`
- Auth: session-based login (`/login` form), then cookie-authenticated API calls
- CSRF: required for state-changing requests (`POST`, `PUT`, `DELETE`)
- Serialization strictness:
  - unknown JSON fields -> `400`
  - unknown query parameters -> `400`

## 2. Common Response Behavior

- Success: JSON unless endpoint explicitly returns file download.
- Validation error: `400` with standard error payload (`message`, optional `details`).
- Auth required: `401` or redirect for browser flow.
- Forbidden (RBAC): `403`.

## 3. Role Matrix (API)

- `ADMIN`: full admin/ops/discovery/recommendation controls.
- `OPS_MANAGER`: operations workflows, analytics reads, discovery, limited writes.
- `MERCHANDISER`: discovery + recommendation read/events, personal workspace.
- `ANALYST`: aggregated analytics read/export only (no personal workspace, no exceptions/task APIs).
- `REGULAR_USER`: personal tasks/notifications + exception create/mine.

## 4. Authentication

### `GET /api/auth/me`
- Auth: authenticated user
- Description: returns current principal and roles.

## 5. Admin APIs

### 5.1 User Management (`/api/admin/users`)
- `GET /api/admin/users`
- `GET /api/admin/users/{userId}`
- `POST /api/admin/users`
- `PUT /api/admin/users/{userId}/roles`
- `PUT /api/admin/users/{userId}/password`
- `PUT /api/admin/users/{userId}/enabled`
- `PUT /api/admin/users/{userId}/profile`
- Auth: `ADMIN`

### 5.2 Audit Logs (`/api/admin/audit-logs`)
- `GET /api/admin/audit-logs`
- Filters include actor/action/date and indexed target filters.
- Auth: `ADMIN`

### 5.3 Notification Admin (`/api/admin/notifications`)
- `GET /templates`
- `PUT /templates/{topic}`
- `GET /definitions`
- `PUT /definitions/{topic}`
- Auth: `ADMIN`

### 5.4 Health (`/api/admin/health`)
- `GET /summary`
- `GET /thresholds`
- `PUT /thresholds/{metricCode}`
- `GET /alerts`
- `PUT /alerts/{alertId}/resolve`
- `GET /errors`
- Auth: `ADMIN`

### 5.5 Recommendation Controls (`/api/admin/recommendations`)
- `GET /config`
- `PUT /config/{key}`
- `GET /stats`
- Auth: `ADMIN`

## 6. Discovery APIs (`/api/discovery`)

- `GET /search`
- `GET /search/lazy` (cursor/keyset pagination with `nextCursor` and `hasMore`)
- `GET /suggestions`
- `GET /trending`
- `GET /history`
- `DELETE /history`
- Auth: `ADMIN`, `MERCHANDISER`, `OPS_MANAGER`

### Discovery Rules (`/api/discovery/rules`)
- `GET /api/discovery/rules`
- `POST /api/discovery/rules`
- `PUT /api/discovery/rules/{id}`
- `DELETE /api/discovery/rules/{id}`
- Auth: `ADMIN`, `MERCHANDISER`

## 7. Recommendations (`/api/recommendations`)

- `GET /api/recommendations`
- `GET /api/recommendations/explain`
- `POST /api/recommendations/events`
- Auth: `ADMIN`, `MERCHANDISER`, `OPS_MANAGER`

Notes:
- Daily per-region per-SKU cap is enforced.
- 24h dedupe is enforced.
- Long-tail policy is enforced by effective served limit calculation under constrained inventory.

## 8. Notifications

### 8.1 User Inbox and Preferences (`/api/notifications`)
- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `PUT /api/notifications/{notificationId}/read`
- `PUT /api/notifications/read-all`
- `GET /api/notifications/preferences`
- `PUT /api/notifications/preferences`
- `GET /api/notifications/subscriptions`
- `PUT /api/notifications/subscriptions`
- Auth: `ADMIN`, `OPS_MANAGER`, `MERCHANDISER`, `REGULAR_USER`

### 8.2 Operational Event Ingestion (`/api/notifications/events`)
- `POST /api/notifications/events/checkin`
- `POST /api/notifications/events/approval-outcome`
- Auth: `ADMIN`, `OPS_MANAGER`

## 9. Tasks (`/api/tasks`)

- `GET /api/tasks/my`
- `PUT /api/tasks/{taskId}/complete`
- `POST /api/tasks` (assignment endpoint)
- Auth:
  - `GET /my`, `PUT /{taskId}/complete`: `ADMIN`, `OPS_MANAGER`, `MERCHANDISER`, `REGULAR_USER`
  - `POST /api/tasks`: `ADMIN`, `OPS_MANAGER`

## 10. Exception Workflow (`/api/exceptions`)

- `POST /api/exceptions`
- `GET /api/exceptions/mine`
- `GET /api/exceptions/pending`
- `PUT /api/exceptions/{id}/decision`
- Auth:
  - create/mine: `ADMIN`, `OPS_MANAGER`, `MERCHANDISER`, `REGULAR_USER`
  - pending/decision: `ADMIN`, `OPS_MANAGER`

## 11. Analytics (`/api/analytics`)

- `GET /api/analytics/kpis`
- `GET /api/analytics/export.csv`
- `GET /api/analytics/export.xlsx`
- `GET /api/analytics/report-packages`
- `GET /api/analytics/report-packages/download?file=...`
- Auth: `ADMIN`, `ANALYST`, `OPS_MANAGER`

Report package contract:
- only strict names are accepted/listed:
  - `kpi-package-YYYYMMDD.csv`
  - `kpi-package-YYYYMMDD.xlsx`

## 12. Security and Contract Guarantees

- Password policy: min 12 chars.
- Lockout: 5 failed attempts -> 15 minute lock.
- Session inactivity timeout: 30 minutes.
- Sensitive fields are encrypted at rest and masked in UI/API responses where applicable.
- API-level validation rejects invalid ranges/types rather than silently normalizing.
