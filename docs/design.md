# Meridian Portal Design

## 1. Purpose
Meridian is an offline-first commerce intelligence portal for on-prem retail operations. It supports:

- product discovery and curation,
- operational workflows (tasks, exceptions, notifications),
- analytics and KPI oversight,
- admin governance and health monitoring.

## 2. Architecture Overview

### 2.1 Runtime
- Backend: Spring Boot (Thymeleaf SSR + REST APIs)
- Database: MySQL (local/on-prem)
- Migration: Flyway
- Packaging: Docker Compose (dev) + production-oriented Docker artifacts

### 2.2 Major Layers
- `controller`: page controllers + REST controllers
- `service`: business logic and policy enforcement
- `repository`: JPA data access
- `security`: authn/authz, session and hardening filters
- `recommendation`, `notification`, `discovery`, `analytics`, `health`: domain modules

### 2.3 Rendering Strategy
- Server-side Thymeleaf for fast first paint
- Progressive enhancement with lightweight JS for:
  - lazy table loading,
  - skeleton states,
  - incremental actions,
  - inline error feedback

## 3. Security Model

- Username/password local auth
- Session-based authentication
- RBAC via role guards (`@PreAuthorize`)
- CSRF on state-changing requests
- Strict request validation:
  - unknown query params rejected
  - unknown JSON fields rejected
  - range/type validation enforced
- Password hardening:
  - min length 12
  - lockout: 5 attempts / 15 minutes
  - session timeout: 30 minutes inactivity
- Data protection:
  - sensitive fields encrypted at rest
  - masked display for protected identifiers

## 4. Core Functional Domains

### 4.1 Discovery
- Multi-filter search (keyword, category, price, condition, time, ZIP-distance)
- Offline ZIP proximity logic
- Type-ahead suggestions (2+ chars)
- 7-day trending searches
- personal history (clearable)
- lazy keyset pagination (`/search/lazy`)

### 4.2 Recommendations
- Event-driven from local actions (search, view, add-to-cart, purchase)
- constraints:
  - 24h dedupe
  - per-region per-SKU daily cap
  - long-tail exposure policy
- admin tuning endpoints and stats

### 4.3 Operations
- Personal tasks and completion flow
- Exception request lifecycle (create, pending queue, decision)
- In-app notification inbox:
  - preferences (DND, reminder caps),
  - topic subscriptions,
  - read/read-all controls

### 4.4 Analytics
- KPI dashboard:
  - GMV, order volume, conversion, AOV, repeat rate, fulfillment timeliness, cancellation reasons
- multidimensional filtering
- CSV/XLSX export
- nightly package listing and download with strict filename contract

### 4.5 Health and Admin Governance
- health summary, thresholds, alert lifecycle, error logs
- user/role administration
- audit logs for admin actions
- recommendation and notification configuration controls

## 5. Data Model (High-Level)

Primary entity groups:
- identity and RBAC: users, roles, user-role mapping
- commerce: products, categories, orders, regions
- operations: tasks, exception requests, workflow events
- notifications: templates, definitions, subscriptions, messages, preferences
- recommendations: events, exposures, controls/stats
- analytics/health: events, KPI aggregates, health logs, thresholds, alerts
- governance: admin audit logs

Schema evolution is maintained via versioned Flyway migrations (`V1`..`V11`).

## 6. UX Design Principles Applied

- fast first paint with SSR,
- explicit role-based navigation,
- concise action-oriented labels,
- visible input validation and failure messages,
- loading affordances (skeletons),
- predictable table/list states (empty/loading/error/success),
- accessible interaction states (`hover`, `active`, `focus-visible`).

## 7. Operations and Environment

### 7.1 Local Dev
- `docker compose up` bootstraps `.env` and runtime config via `scripts/bootstrap_runtime.sh`.
- app runs on `${APP_HOST_PORT}` (default `18080`).

### 7.2 Test Execution
- `./run_test.sh`:
  - bootstraps environment,
  - resets isolated test DB,
  - runs full suite in Docker,
  - prints pass/fail/error summary.

### 7.3 Key Docs
- API contract: `docs/api-spec.md`
- release and rollback: `docs/ops/*`
- key management: `docs/security/key_management_policy.md`
- UAT evidence: `docs/uat/uat_signoff_evidence.md`

## 8. Current Intentional Boundaries

- Analysts are constrained to aggregated analytics surfaces (no personal task/notification pages or exception queues).
- Report package listing/download only accepts strict KPI package naming.
- Recommendation serving may reduce served count under constrained eligibility to preserve policy constraints.
