#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DB_NAME="${MERIDIAN_TEST_DB_NAME:-meridian_test}"
DB_APP_USER="${MYSQL_USER:-meridian_user}"

echo "[run_test] bootstrapping .env and runtime config"
"${ROOT_DIR}/scripts/bootstrap_runtime.sh"

echo "[run_test] ensuring mysql is up"
docker compose up -d mysql

echo "[run_test] resetting isolated test database: ${TEST_DB_NAME}"
docker compose exec -T mysql sh -c \
  "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -e \"\
DROP DATABASE IF EXISTS ${TEST_DB_NAME}; \
CREATE DATABASE ${TEST_DB_NAME}; \
GRANT ALL PRIVILEGES ON ${TEST_DB_NAME}.* TO '${DB_APP_USER}'@'%'; \
FLUSH PRIVILEGES;\""

echo "[run_test] running full backend test suite in docker (isolated db)"
set +e
docker compose run --rm \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql:3306/${TEST_DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Africa/Addis_Ababa" \
  app-dev sh -c "mvn test"
MVN_EXIT_CODE=$?
set -e

REPORT_DIR="${ROOT_DIR}/backend/target/surefire-reports"
if [ -d "${REPORT_DIR}" ]; then
  TESTS=$(grep -h "<testsuite " "${REPORT_DIR}"/TEST-*.xml 2>/dev/null | sed -n 's/.* tests="\([0-9]\+\)".*/\1/p' | awk '{s+=$1} END{print s+0}')
  FAILURES=$(grep -h "<testsuite " "${REPORT_DIR}"/TEST-*.xml 2>/dev/null | sed -n 's/.* failures="\([0-9]\+\)".*/\1/p' | awk '{s+=$1} END{print s+0}')
  ERRORS=$(grep -h "<testsuite " "${REPORT_DIR}"/TEST-*.xml 2>/dev/null | sed -n 's/.* errors="\([0-9]\+\)".*/\1/p' | awk '{s+=$1} END{print s+0}')
  SKIPPED=$(grep -h "<testsuite " "${REPORT_DIR}"/TEST-*.xml 2>/dev/null | sed -n 's/.* skipped="\([0-9]\+\)".*/\1/p' | awk '{s+=$1} END{print s+0}')
  PASSED=$((TESTS - FAILURES - ERRORS - SKIPPED))
  echo "[run_test] summary: Passed=${PASSED} Failed=${FAILURES} Errors=${ERRORS} Skipped=${SKIPPED} Total=${TESTS}"
else
  echo "[run_test] summary: surefire reports not found at ${REPORT_DIR}"
fi

exit "${MVN_EXIT_CODE}"
