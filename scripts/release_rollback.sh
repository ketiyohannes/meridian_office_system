#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" = "" ]; then
  echo "Usage: $0 <previous-version-tag>"
  echo "Example: $0 v0.9.3"
  exit 1
fi

PREV_VERSION="$1"
IMAGE_REPO="${MERIDIAN_IMAGE_REPO:-meridian/portal}"
export MERIDIAN_APP_IMAGE="${IMAGE_REPO}"
export MERIDIAN_APP_VERSION="${PREV_VERSION}"

echo "Rolling back to image ${MERIDIAN_APP_IMAGE}:${MERIDIAN_APP_VERSION}"
docker compose -f docker-compose.prod.yml up -d app
echo "Rollback command complete. Verify /api/auth/me and /api/admin/health/summary after rollback."
