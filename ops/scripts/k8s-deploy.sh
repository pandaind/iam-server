#!/usr/bin/env bash
# ------------------------------------------------------------------------------
# k8s-deploy.sh -- Deploy IAM Server to Kubernetes
#
# Supports three environments via Kustomize overlay directories:
#   ops/k8s/overlays/dev/        local / dev cluster
#   ops/k8s/overlays/staging/    staging cluster
#   ops/k8s/overlays/prod/       production cluster
#
# Usage:
#   ./k8s-deploy.sh -e <env> [OPTIONS] [COMMAND]
#
# Commands:  apply (default) | delete | status | rollback
#
# Options:
#   -e | --env ENV          Target environment: dev | staging | prod  (required)
#   -t | --tag TAG          Image tag to deploy                (default: latest)
#   -c | --context CTX      kubectl context                (default: current)
#   -n | --namespace NS     Override namespace              (default: iam-system)
#        --dry-run          Print manifests without applying
#   -h | --help
#
# Examples:
#   ./k8s-deploy.sh -e dev apply
#   ./k8s-deploy.sh -e prod apply -t v1.2.0 -c prod-cluster
#   ./k8s-deploy.sh -e prod rollback
#   ./k8s-deploy.sh -e staging status
# ------------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
K8S_DIR="${SCRIPT_DIR}/../k8s"
ENV=""
TAG="latest"
KUBE_CONTEXT=""
NAMESPACE="iam-system"
DRY_RUN=""
COMMAND="apply"

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; B='\033[0;34m'; N='\033[0m'
info()  { echo -e "${B}[info]${N}  $*"; }
ok()    { echo -e "${G}[ok]${N}    $*"; }
warn()  { echo -e "${Y}[warn]${N}  $*"; }
fatal() { echo -e "${R}[error]${N} $*" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
  case $1 in
    -e|--env)       ENV="$2";          shift 2 ;;
    -t|--tag)       TAG="$2";          shift 2 ;;
    -c|--context)   KUBE_CONTEXT="$2"; shift 2 ;;
    -n|--namespace) NAMESPACE="$2";    shift 2 ;;
    --dry-run)      DRY_RUN="--dry-run=client"; shift ;;
    apply|delete|status|rollback) COMMAND="$1"; shift ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^# \{0,1\}//' | head -30; exit 0 ;;
    *) fatal "Unknown option: $1" ;;
  esac
done

[[ -n "${ENV}" ]] || fatal "-e/--env is required. Use: dev | staging | prod"
[[ "${ENV}" =~ ^(dev|staging|prod)$ ]] || fatal "env must be dev, staging, or prod"

OVERLAY="${K8S_DIR}/overlays/${ENV}"

command -v kubectl &>/dev/null || fatal "kubectl not found in PATH"

if [[ -n "${KUBE_CONTEXT}" ]]; then
  info "Switching kubectl context to: ${KUBE_CONTEXT}"
  kubectl config use-context "${KUBE_CONTEXT}"
fi

info "Target env: ${ENV} | namespace: ${NAMESPACE} | tag: ${TAG}"

if [[ "${ENV}" == "prod" && "${COMMAND}" == "delete" ]]; then
  warn "You are about to DELETE all resources in the PROD namespace!"
  read -rp "Type 'yes' to confirm: " confirm
  [[ "${confirm}" == "yes" ]] || { info "Aborted."; exit 0; }
fi

apply_manifests() {
  if [[ -f "${OVERLAY}/kustomization.yaml" ]]; then
    info "Using Kustomize overlay: ${OVERLAY}"
    kubectl kustomize "${OVERLAY}" \
      | sed -e "s|iam-server:latest|iam-server:${TAG}|g" \
            -e "s|iam-frontend:latest|iam-frontend:${TAG}|g" \
      | kubectl apply ${DRY_RUN} -f -
  else
    warn "No overlay found at ${OVERLAY} -- applying base manifests"
    kubectl apply ${DRY_RUN} -f "${K8S_DIR}/namespace.yaml"
    for f in "${K8S_DIR}"/*.yaml; do
      [[ "$(basename "$f")" == "namespace.yaml" ]] && continue
      sed -e "s|iam-server:1.0.0|iam-server:${TAG}|g" \
          -e "s|iam-frontend:1.0.0|iam-frontend:${TAG}|g" \
          "$f" | kubectl apply ${DRY_RUN} -f -
    done
  fi
}

case "${COMMAND}" in
  apply)
    info "Deploying to ${ENV} ..."
    apply_manifests
    if [[ -z "${DRY_RUN}" ]]; then
      info "Waiting for rollout (timeout: 300s) ..."
      kubectl rollout status deployment/iam-server -n "${NAMESPACE}" --timeout=300s
      ok "Deployment complete (env: ${ENV}, tag: ${TAG})"
    else
      ok "Dry-run complete -- no changes applied"
    fi
    ;;
  delete)
    info "Deleting ${ENV} resources ..."
    if [[ -f "${OVERLAY}/kustomization.yaml" ]]; then
      kubectl kustomize "${OVERLAY}" | kubectl delete ${DRY_RUN} -f - --ignore-not-found
    else
      kubectl delete ${DRY_RUN} -f "${K8S_DIR}" --ignore-not-found
    fi
    ok "Resources deleted"
    ;;
  status)
    echo
    echo "-- Namespace: ${NAMESPACE} -------------------------------------------------"
    kubectl get pods,svc,ingress -n "${NAMESPACE}" 2>/dev/null \
      || warn "Namespace ${NAMESPACE} not found or empty"
    echo
    echo "-- Rollout status ----------------------------------------------------------"
    kubectl rollout status deployment/iam-server -n "${NAMESPACE}" 2>/dev/null || true
    echo
    echo "-- HPA ---------------------------------------------------------------------"
    kubectl get hpa -n "${NAMESPACE}" 2>/dev/null || echo "  (no HPA found)"
    ;;
  rollback)
    warn "Rolling back iam-server in ${ENV} ..."
    kubectl rollout undo deployment/iam-server -n "${NAMESPACE}"
    kubectl rollout status deployment/iam-server -n "${NAMESPACE}" --timeout=300s
    ok "Rollback complete"
    ;;
  *)
    fatal "Unknown command: ${COMMAND}. Use apply | delete | status | rollback"
    ;;
esac
