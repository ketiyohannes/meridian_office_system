# Meridian Phase 1 UAT Sign-Off Evidence

Date: 2026-03-26  
Environment: local Dockerized on-prem simulation (`docker compose`, MySQL 8.4, Spring Boot backend)

## Execution Record
- Command executed: `cd backend && mvn -q test`
- Result: PASS (`exit code 0`)
- JUnit report: `backend/target/surefire-reports/TEST-com.meridian.portal.integration.UatSignOffIntegrationTest.xml`
- UAT suite totals: `tests=5`, `failures=0`, `errors=0`

## Role/Workflow Acceptance Evidence

1. Admin workflow
- Test: `adminWorkflow_userManagementAndHealthAndRecommendationControls`
- Evidence:
  - Admin can create users with role assignment.
  - Admin can access `/api/admin/health/summary`.
  - Admin can access recommendation config endpoints.
- Status: ACCEPTED

2. Merchandiser workflow
- Test: `merchandiserWorkflow_discoveryAndRecommendations`
- Evidence:
  - Merchandiser can search discovery inventory with results.
  - Merchandiser can retrieve recommendations for search surface.
- Status: ACCEPTED

3. Regular user workflow
- Test: `regularUserWorkflow_notificationsAndPreferences`
- Evidence:
  - Regular user can update notification preferences (DND/reminder cap).
  - Regular user receives approval outcome events in notification feed.
- Status: ACCEPTED

4. Analyst workflow
- Test: `analystWorkflow_kpiDashboardAndExports`
- Evidence:
  - Analyst can access KPI aggregates.
  - Analyst can export CSV analytics view.
- Status: ACCEPTED

5. Ops manager workflow
- Test: `opsManagerWorkflow_exceptionEventAndAnalyticsAccess`
- Evidence:
  - Ops manager can create approval outcome events.
  - Ops manager can access analytics KPIs.
  - Unauthorized regular-user action (`DELETE /api/discovery/history`) is denied with `403`.
- Status: ACCEPTED

## Sign-Off Summary
- Phase 1 UAT scenarios for Admin, Merchandiser, Regular User, Analyst, and Ops Manager have passed in automated integration/UAT runs.
- Security behavior verified in UAT: RBAC denial paths return forbidden responses and do not leak server internals.
