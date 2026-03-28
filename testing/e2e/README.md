# E2E Tests (Playwright)

End-to-end tests covering the full IAM stack — API flows and browser-based UI tests — using [Playwright](https://playwright.dev).

## Test structure

```
tests/
├── global.setup.ts          # Authenticates admin and persists token to .auth/
├── helpers.ts               # Shared utilities (auth injection, navigation)
├── auth.spec.ts             # Authentication API tests (login, refresh, 401s)
├── users.spec.ts            # User CRUD API tests
├── roles-permissions.spec.ts # Role & Permission API tests
├── audit-sessions.spec.ts   # Audit log & session API tests
├── ui-login.spec.ts         # Browser tests: login form, redirect, error states
└── ui-pages.spec.ts         # Browser tests: dashboard, users, roles, etc.
```

## Prerequisites

- Node.js 18+
- A running backend: `make compose-up` or `make dev-run`
- A running frontend: `cd frontend && npm run dev`

## Setup

```bash
cd testing/e2e
npm install
npm run install:browsers   # downloads Chromium only
```

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:3000` | Frontend URL |
| `API_URL` | `http://localhost:8080/api/v1` | Backend API base URL |
| `ADMIN_USER` | `admin` | Admin username |
| `ADMIN_PASS` | `admin123` | Admin password |

## Running

```bash
# Run all tests (headless)
npm test

# Interactive UI mode
npm run test:ui

# Headed browser (watch tests run)
npm run test:headed

# Debug mode (step through)
npm run test:debug

# CI mode (GitHub Actions + JUnit XML output)
npm run test:ci

# View HTML report from last run
npm run test:report
```

## Test categories

### API tests (`auth.spec.ts`, `users.spec.ts`, `roles-permissions.spec.ts`, `audit-sessions.spec.ts`)
- Use Playwright's `request` fixture to hit the backend directly
- No browser required for these tests
- Cover all CRUD operations, auth flows, and access control (401/403)

### UI tests (`ui-login.spec.ts`, `ui-pages.spec.ts`)
- Launch a real Chromium browser
- Inject auth tokens via `localStorage` for protected-page tests
- Test login form, redirect flows, and page rendering

## Artifacts

After a test run, the following are generated:
- `playwright-report/` — HTML report (open with `npm run test:report`)
- `results/junit.xml` — JUnit XML for CI integration
- `test-results/` — Screenshots and traces on failure
