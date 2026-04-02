#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
ARTIFACT_DIR="${ROOT_DIR}/release/artifacts"
mkdir -p "${ARTIFACT_DIR}"

VERSION="${1:-}"
if [ -z "${VERSION}" ]; then
  echo "Usage: $0 <version-tag>"
  echo "Example: $0 v1.0.0"
  exit 1
fi

IMAGE_REPO="${MERIDIAN_IMAGE_REPO:-meridian/portal}"
IMAGE_TAG="${IMAGE_REPO}:${VERSION}"

echo "[1/3] Building backend jar..."
(
  cd "${BACKEND_DIR}"
  mvn -q -DskipTests package
)

echo "[2/3] Building Docker image ${IMAGE_TAG}..."
docker build -t "${IMAGE_TAG}" "${BACKEND_DIR}"

echo "[3/3] Saving image artifact..."
docker save "${IMAGE_TAG}" | gzip > "${ARTIFACT_DIR}/meridian-portal-${VERSION}.tar.gz"

echo "Release artifact ready:"
echo "  Image: ${IMAGE_TAG}"
echo "  Tarball: ${ARTIFACT_DIR}/meridian-portal-${VERSION}.tar.gz"
