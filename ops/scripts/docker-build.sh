#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# docker-build.sh — Build and optionally push IAM Server Docker images
#
# Usage:
#   ./docker-build.sh [OPTIONS]
#
# Options:
#   -t | --tag TAG          Image tag            (default: latest)
#   -r | --registry REG     Registry prefix      (e.g. ghcr.io/myorg)
#   -p | --push             Push after build
#        --no-cache         Build without cache
#   -h | --help
#
# Examples:
#   ./docker-build.sh -t v1.2.0 -r ghcr.io/vibes -p
#   ./docker-build.sh --no-cache
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TAG="latest"
REGISTRY=""
PUSH=false
NO_CACHE=""

# ── Colours ───────────────────────────────────────────────────────────────────
R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; B='\033[0;34m'; N='\033[0m'
info()  { echo -e "${B}[info]${N}  $*"; }
ok()    { echo -e "${G}[ok]${N}    $*"; }
warn()  { echo -e "${Y}[warn]${N}  $*"; }
fatal() { echo -e "${R}[error]${N} $*" >&2; exit 1; }

# ── Arg parsing ───────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case $1 in
    -t|--tag)       TAG="$2";      shift 2 ;;
    -r|--registry)  REGISTRY="$2"; shift 2 ;;
    -p|--push)      PUSH=true;     shift ;;
    --no-cache)     NO_CACHE="--no-cache"; shift ;;
    -h|--help)
      sed -n '/^# Usage/,/^# ──/p' "$0" | head -n -1
      exit 0 ;;
    *) fatal "Unknown option: $1" ;;
  esac
done

# ── Pre-flight ────────────────────────────────────────────────────────────────
command -v docker &>/dev/null || fatal "docker not found"
docker info &>/dev/null       || fatal "Docker daemon not running"

# ── Build ─────────────────────────────────────────────────────────────────────
build_image() {
  local name="$1" context="$2" dockerfile="$3"
  local local_tag="${name}:${TAG}"
  local full_tag="${REGISTRY:+${REGISTRY}/}${local_tag}"

  info "Building ${full_tag} …"
  docker build ${NO_CACHE} \
    -t "${local_tag}" \
    -f "${dockerfile}" \
    "${context}"
  ok "Built ${local_tag}"

  if [[ -n "${REGISTRY}" && "${full_tag}" != "${local_tag}" ]]; then
    docker tag "${local_tag}" "${full_tag}"
    ok "Tagged  ${full_tag}"
  fi

  if [[ "${PUSH}" == true ]]; then
    [[ -n "${REGISTRY}" ]] || fatal "--push requires --registry"
    info "Pushing ${full_tag} …"
    docker push "${full_tag}"
    ok "Pushed  ${full_tag}"
  fi
}

build_image "iam-server"   "${ROOT}/backend"  "Dockerfile"
build_image "iam-frontend" "${ROOT}/frontend" "Dockerfile"

echo
ok "All images built. Tag: ${TAG}"
[[ "${PUSH}" == true ]] && ok "Pushed to: ${REGISTRY:-local}"
echo
info "Next:"
echo "  Local dev:    cd ops && docker compose up -d"
echo "  K8s deploy:   ops/scripts/k8s-deploy.sh -e prod -t ${TAG}"
