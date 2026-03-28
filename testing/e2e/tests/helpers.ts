import { Page, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

export const BASE_URL = process.env.BASE_URL ?? 'http://localhost:3000';
export const API_URL = process.env.API_URL ?? 'http://localhost:8080/api/v1';

export function getAdminAuth(): { accessToken: string; user: { username: string } } {
  const filePath = path.join(__dirname, '../.auth/admin.json');
  if (!fs.existsSync(filePath)) {
    throw new Error('Admin auth state not found. Run global setup first.');
  }
  return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
}

/** Inject the Bearer token into localStorage so the app treats us as logged in. */
export async function injectAuthToken(page: Page): Promise<void> {
  const { accessToken, user } = getAdminAuth();
  await page.addInitScript(
    ({ token, userData }) => {
      localStorage.setItem('accessToken', token);
      localStorage.setItem('currentUser', JSON.stringify(userData));
    },
    { token: accessToken, userData: user },
  );
}

/** Navigate to a page and wait for the main content to be visible. */
export async function gotoProtected(page: Page, path: string): Promise<void> {
  await injectAuthToken(page);
  await page.goto(path);
  // Wait for the sidebar to render (indicates layout loaded)
  await expect(page.locator('text=IAM Console').or(page.locator('text=IAM'))).toBeVisible({
    timeout: 10_000,
  });
}
