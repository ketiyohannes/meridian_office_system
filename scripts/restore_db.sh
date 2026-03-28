#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" = "" ]; then
  echo "Usage: $0 /path/to/backup.sql.gz"
  exit 1
fi

BACKUP_FILE="$1"
if [ ! -f "${BACKUP_FILE}" ]; then
  echo "Backup file does not exist: ${BACKUP_FILE}"
  exit 1
fi

DB_HOST="${SPRING_DATASOURCE_HOST:-localhost}"
DB_PORT="${SPRING_DATASOURCE_PORT:-3307}"
DB_NAME="${SPRING_DATASOURCE_DB:-meridian}"
DB_USER="${SPRING_DATASOURCE_USERNAME:-meridian_user}"
DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-meridian_password}"

gunzip -c "${BACKUP_FILE}" | mysql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  "${DB_NAME}"

echo "Restore completed from: ${BACKUP_FILE}"
