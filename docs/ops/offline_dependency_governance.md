# Offline Dependency Governance

## Objective
Ensure Meridian can be built and deployed in internet-restricted on-prem environments.

## Policy
1. No runtime dependency on external internet services for core workflows.
2. Build dependencies must be sourced from approved internal artifact mirrors.
3. Docker base images must be promoted into an internal registry before release.

## Build Controls
1. Mirror Maven dependencies to internal repository manager.
2. Configure `settings.xml` to resolve from internal mirrors only in production build lanes.
3. Freeze dependency versions for release branches.

## Verification Checklist
1. Run application services with outbound network blocked.
2. Validate auth, discovery, notifications, analytics, recommendations still function.
3. Validate scheduled jobs (notifications/report packages) complete without external calls.

## Incident Handling
1. If any module attempts external connectivity in production, treat as release blocker.
2. Patch to remove external dependency and re-run offline verification.
