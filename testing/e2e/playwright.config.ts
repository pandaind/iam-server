import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for IAM Server E2E tests.
 * Requires a running stack: `make compose-up` + `cd frontend && npm run dev`
 *
 * Environment variables:
 *   BASE_URL      — UI base URL (default: http://localhost:3000)
 *   API_URL       — Backend API base URL (default: http://localhost:8080/api/v1)
 *   ACTUATOR_URL  — Actuator base URL (default: http://localhost:8080/api/v1/actuator)
 *   ADMIN_USER    — Admin username (default: admin)
 *   ADMIN_PASS    — Admin password (default: Admin@123)
 */
export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  expect: { timeout: 8_000 },
  fullyParallel: false, // IAM state mutations — run serially
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'results/junit.xml' }],
    ['list'],
  ],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
    },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['setup'],
    },
  ],
});
