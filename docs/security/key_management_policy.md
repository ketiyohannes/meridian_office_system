# Meridian Key Management Policy (Offline/On-Prem)

## Scope
This policy applies to:
- `MERIDIAN_DATA_ENCRYPTION_KEY`
- `MERIDIAN_BOOTSTRAP_ADMIN_PASSWORD`
- MySQL credentials (`MYSQL_*`)

## Requirements
1. `MERIDIAN_DATA_ENCRYPTION_KEY` must be exactly 32 characters.
2. No default keys/passwords are allowed in production.
3. Keys and secrets must be injected through environment management, never committed to source control.
4. Production uses `SPRING_PROFILES_ACTIVE=prod`; startup hardening checks will fail on weak/default values.

## Rotation Schedule
1. Data encryption key:
   - Rotate every 180 days or on incident.
   - Rotation must be coordinated with data re-encryption maintenance (if key changes, encrypted fields must be re-written).
2. Bootstrap admin password:
   - Rotate every 90 days.
   - Disable bootstrap credential reuse after first secure admin setup.
3. Database credentials:
   - Rotate every 90 days.

## Storage/Custody
1. Store production secrets in approved internal secret custody process (vault, keystore, or controlled encrypted env files).
2. Access is limited to designated platform operators.
3. All secret access must be auditable.

## Emergency Handling
1. If key leakage is suspected, rotate immediately.
2. Revoke affected credentials.
3. Record incident timeline and recovery actions.
