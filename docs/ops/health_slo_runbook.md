# Health SLO Runbook

## SLO Definition
1. HTTP 5xx rate threshold:
   - Alert when 5xx responses exceed 1% over rolling 15 minutes.

## Monitor Locations
1. Admin Health UI:
   - `/admin/health`
2. APIs:
   - `/api/admin/health/summary`
   - `/api/admin/health/alerts`
   - `/api/admin/health/errors`

## Operational Response
1. Alert opened:
   - Validate affected endpoints from error logs.
   - Check recent deploy/version change.
2. If user-facing authentication or RBAC errors are involved:
   - Trigger rollback decision immediately.
3. If isolated workflow failure:
   - Resolve root cause and mark alert resolved after recovery.

## Escalation Criteria
1. 5xx > 1% for more than 15 minutes.
2. Repeated unresolved alerts within 24 hours.
3. Any outage in auth, admin, or order-critical flows.

## Secure Logging Guideline
1. Do not write raw request payload fragments, credentials, or user-entered secrets into `health_error_logs.details_json`.
2. For malformed JSON and parser failures, persist only a fixed reason code (for example `MALFORMED_JSON`).
3. Keep user-facing API error semantics stable while minimizing internal log sensitivity.
