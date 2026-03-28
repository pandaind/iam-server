import { test, expect } from '@playwright/test';
import { gotoProtected } from './helpers';

const adminCreds = {
  username: process.env.ADMIN_USER ?? 'admin',
  password: process.env.ADMIN_PASS ?? 'Admin@123',
};

test.describe('Login Page', () => {
  test('shows login form on /login', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByText('IAM Management Console')).toBeVisible();
    await expect(page.getByPlaceholder('Username')).toBeVisible();
    await expect(page.getByPlaceholder('Password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible();
  });

  test('shows error on invalid credentials', async ({ page }) => {
    await page.goto('/login');
    // Wait for full hydration
    await page.waitForLoadState('networkidle');
    const signInBtn = page.getByRole('button', { name: 'Sign In' });
    await expect(signInBtn).toBeEnabled({ timeout: 10_000 });
    // Use explicit input locators to avoid AntD wrapper confusion
    await page.locator('input[placeholder="Username"]').fill('invalid_user');
    await page.locator('input[placeholder="Password"]').fill('wrong_password');
    await signInBtn.click();
    // After the API call fails, an error alert should appear
    await expect(page.locator('.ant-alert-error')).toBeVisible({ timeout: 10_000 });
  });

  test('redirects to /dashboard after successful login', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Username').fill(adminCreds.username);
    await page.getByPlaceholder('Password').fill(adminCreds.password);
    await page.getByRole('button', { name: 'Sign In' }).click();
    await page.waitForURL('**/dashboard', { timeout: 15_000 });
    await expect(page).toHaveURL(/\/dashboard/);
  });
});

test.describe('Protected Redirect', () => {
  test('unauthenticated visit to /dashboard redirects to /login', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForURL('**/login', { timeout: 10_000 });
    await expect(page).toHaveURL(/\/login/);
  });
});
