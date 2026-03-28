# API Tests (Postman + Newman)

This directory contains API-level tests exercising the IAM Server HTTP endpoints as a black-box service using Postman collections and Newman.

## Files

| File | Description |
|------|-------------|
| `iam-server.postman_collection.json` | Full Postman collection (8 folders, ~30 requests) |
| `iam-local.postman_environment.json` | Environment for local dev (`localhost:8080`) |
| `iam-docker.postman_environment.json` | Environment for Docker Compose stack |
| `package.json` | Newman runner dependencies & scripts |

## Prerequisites

- Node.js 18+
- A running IAM Server (`make compose-up` or `make dev-run`)

## Setup

```bash
cd testing/api
npm install
```

## Running

```bash
# Run against local dev server (HTML report in results/)
npm test

# Run against Docker Compose stack
npm run test:docker

# CI mode — bail on first failure, output JUnit XML
npm run test:ci
```

Or directly with Newman:
```bash
npx newman run iam-server.postman_collection.json \
  -e iam-local.postman_environment.json \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export results/report.html
```

## Collection structure

| Folder | Requests |
|--------|----------|
| 01 - Health | Public health, Actuator health |
| 02 - Authentication | Login, Refresh, Invalid creds, Missing fields |
| 03 - Users | CRUD + enable/disable lifecycle |
| 04 - Roles | CRUD + activate/deactivate |
| 05 - Permissions | CRUD |
| 06 - Sessions | Get my sessions |
| 07 - Audit Logs | All, failed, by action |
| 08 - Logout | Token invalidation |

## Import into Postman

1. Open Postman → **Import**
2. Select `iam-server.postman_collection.json`
3. Also import `iam-local.postman_environment.json`
4. Select the **IAM Local** environment and run the collection

## Notes on Spring Boot integration tests

The Maven integration tests in `backend/src/test/java/.../controller/` use MockMvc/TestContainers and run with:

```bash
make test-integration
```
