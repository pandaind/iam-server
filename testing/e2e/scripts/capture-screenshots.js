// Run: node scripts/capture-screenshots.js
const { chromium } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:3000';
const OUT_DIR  = path.resolve(__dirname, '../../../docs/screenshots');

async function login(page) {
  await page.goto(BASE_URL + '/login');
  await page.waitForLoadState('networkidle');
  await page.locator('input[placeholder="Username"]').fill('admin');
  await page.locator('input[placeholder="Password"]').fill('Admin@123');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL(BASE_URL + '/dashboard', { timeout: 15000 });
}

async function shot(page, name, waitFn) {
  if (waitFn) await waitFn();
  await page.screenshot({ path: path.join(OUT_DIR, name + '.png'), fullPage: false });
  console.log('  saved', name + '.png');
}

(async () => {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const p = await ctx.newPage();

  // 01 – Login page
  await p.goto(BASE_URL + '/login');
  await p.waitForLoadState('networkidle');
  await shot(p, '01-login');

  // 02 – Login error state
  await p.locator('input[placeholder="Username"]').fill('bad_user');
  await p.locator('input[placeholder="Password"]').fill('wrongpass');
  await p.getByRole('button', { name: 'Sign In' }).click();
  await p.waitForSelector('.ant-alert-error', { timeout: 8000 });
  await shot(p, '02-login-error');

  // Authenticate
  await login(p);

  // 03 – Dashboard
  await p.waitForSelector('.ant-statistic', { timeout: 10000 });
  await shot(p, '03-dashboard');

  // 04 – Users
  await p.goto(BASE_URL + '/users');
  await p.waitForSelector('.ant-table-tbody', { timeout: 10000 });
  await shot(p, '04-users');

  // 05 – Roles
  await p.goto(BASE_URL + '/roles');
  await p.waitForSelector('.ant-table-tbody', { timeout: 10000 });
  await shot(p, '05-roles');

  // 06 – Permissions
  await p.goto(BASE_URL + '/permissions');
  await p.waitForSelector('.ant-table-tbody', { timeout: 10000 });
  await shot(p, '06-permissions');

  // 07 – Audit logs
  await p.goto(BASE_URL + '/audit');
  await p.waitForSelector('.ant-table-tbody', { timeout: 10000 });
  await shot(p, '07-audit-logs');

  // 08 – Sessions
  await p.goto(BASE_URL + '/sessions');
  await p.waitForLoadState('networkidle');
  await shot(p, '08-sessions');

  // 09 – Settings
  await p.goto(BASE_URL + '/settings');
  await p.waitForLoadState('networkidle');
  await shot(p, '09-settings');

  await browser.close();
  console.log('\nAll screenshots saved to', OUT_DIR);
})().catch(e => { console.error(e); process.exit(1); });
