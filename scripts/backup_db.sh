#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${ROOT_DIR}/backups"
mkdir -p "${BACKUP_DIR}"

TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
OUTPUT_FILE="${BACKUP_DIR}/meridian_${TIMESTAMP}.sql.gz"

DB_HOST="${SPRING_DATASOURCE_HOST:-localhost}"
DB_PORT="${SPRING_DATASOURCE_PORT:-3307}"
DB_NAME="${SPRING_DATASOURCE_DB:-meridian}"
DB_USER="${SPRING_DATASOURCE_USERNAME:-meridian_user}"
DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-meridian_password}"

mysqldump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --single-transaction \
  --quick \
  --routines \
  --events \
  "${DB_NAME}" | gzip > "${OUTPUT_FILE}"

echo "Backup created: ${OUTPUT_FILE}"
