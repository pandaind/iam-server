# Frontend

This directory is reserved for the IAM Server management UI.

## Current State

The backend currently serves two server-side rendered Thymeleaf pages (login and OAuth2 consent) located in `backend/src/main/resources/templates/`. These are minimal pages required by the Spring Authorization Server flow and are not part of a standalone frontend application.

## Planned UI

See [IAM_UI_MANAGEMENT_PLAN.md](../IAM_UI_MANAGEMENT_PLAN.md) for the full management console plan.

Planned stack (subject to change):
- **Framework**: React or Vue.js
- **Features**: User management, role/permission management, audit log viewer, session management
- **Auth**: Integrates with the IAM server's OAuth2/OIDC endpoints

## Getting Started (once implemented)

```bash
cd frontend
npm install
npm run dev
```
