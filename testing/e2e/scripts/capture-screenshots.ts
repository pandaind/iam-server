/**
 * Capture product screenshots for the README.
 * Run:  npx ts-node --esm scripts/capture-screenshots.ts
 * Or:   npx playwright test scripts/capture-screenshots.ts --headed
 *
 * Output: docs/screenshots/ (relative to repo root)
 */
import { chromium } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const BASE_URL = 'http://localhost:3000';
const API_URL  = 'http://localhost:8080/api/v1';
const OUT_DIR  = path.resolve(__dirname, '../../../docs/screenshots');

async function login(page: import('@playwright/test').Page) {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');
  await page.locator('input[placeholder="Username"]').fill('admin');
  await page.locator('input[placeholder="Password"]').fill('Admin@123');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL(`${BASE_URL}/dashboard`, { timeout: 15_000 });
}

async function shot(
  page: import('@playwright/test').Page,
  name: string,
  waitFor?: () => Promise<void>,
) {
  if (waitFor) await waitFor();
  await page.screenshot({
    path: path.join(OUT_DIR, `${name}.png`),
    fullPage: false,
  });
  console.log(`  ✓ ${name}.png`);
}

(async () => {
  fs.mkdirSync(OUT_DIR, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
  });
  const page = await context.newPage();

  // ── 1. Login page ─────────────────────────────────────────────────────────
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('networkidle');
  await shot(page, '01-login');

  // ── 2. Login error state ───────────────────────────────────────────────────
  await page.locator('input[placeholder="Username"]').fill('bad_user');
  await page.locator('input[placeholder="Password"]').fill('wrong');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForSelector('.ant-alert-error', { timeout: 8_000 });
  await shot(page, '02-login-error');

  // ── 3. Authenticate and capture dashboard ─────────────────────────────────
  await login(page);
  await shot(page, '03-dashboard', async () => {
    await page.waitForSelector('.ant-statistic', { timeout: 10_000 });
  });

  // ── 4. Users page ─────────────────────────────────────────────────────────
  await page.goto(`${BASE_URL}/users`);
  await page.waitForSelector('.ant-table-tbody', { timeout: 10_000 });
  await shot(page, '04-users');

  // ── 5. Create user modal ───────────────────────────────────────────────────
  await page.getByRole('button', { name: 'Create User' }).click();
  await page.waitForSelector('.ant-modal-content', { timeout: 5_000 });
  await shot(page, '05-create-user-modal');
  await page.keyboard.press('Escape');

  // ── 6. Roles page ─────────────────────────────────────────────────────────
  await page.goto(`${BASE_URL}/roles`);
  await page.waitForSelector('.ant-table-tbody', { timeout: 10_000 });
  await shot(page, '06-roles');

  // ── 7. Permissions page ───────────────────────────────────────────────────
  await page.goto(`${BASE_URL}/permissions`);
  await page.waitForSelector('.ant-table-tbody', { timeout: 10_000 });
  await shot(page, '07-permissions');

  // ── 8. Audit logs page ────────────────────────────────────────────────────
  await page.goto(`${BASE_URL}/audit`);
  await page.waitForSelector('.ant-table-tbody', { timeout: 10_000 });
  await shot(page, '08-audit-logs');

  // ── 9. Sessions page ──────────────────────────────────────────────────────
  await page.goto(`${BASE_URL}/sessions`);
  await page.waitForLoadState('networkidle');
  await shot(page, '09-sessions');

  // ── 10. Settings page ─────────────────────────────────────────────────────
  await page.goto(`${BASE_URL}/settings`);
  await page.waitForLoadState('networkidle');
  await shot(page, '10-settings');

  await browser.close();
  console.log(`\nAll screenshots saved to ${OUT_DIR}`);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
