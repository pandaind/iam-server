import { test, expect } from '@playwright/test';
import { gotoProtected } from './helpers';

test.describe('Dashboard Page', () => {
  test('shows welcome message and stat cards', async ({ page }) => {
    await gotoProtected(page, '/dashboard');
    await expect(page.getByText(/Welcome back/i)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('Total Users')).toBeVisible();
    await expect(page.getByText('Active Roles')).toBeVisible();
    await expect(page.getByText('My Active Sessions')).toBeVisible();
    await expect(page.getByText('Recent Audit Events')).toBeVisible();
  });

  test('sidebar shows navigation links', async ({ page }) => {
    await gotoProtected(page, '/dashboard');
    await expect(page.getByRole('menuitem', { name: 'Users' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Roles' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Permissions' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Sessions' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Audit Logs' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Settings' })).toBeVisible();
  });
});

test.describe('Users Page', () => {
  test('renders user table', async ({ page }) => {
    await gotoProtected(page, '/users');
    await expect(page.getByText('User Management')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('button', { name: 'New User' })).toBeVisible();
    await expect(page.getByPlaceholder('Search by username or email')).toBeVisible();
  });

  test('open and close create user modal', async ({ page }) => {
    await gotoProtected(page, '/users');
    await page.getByRole('button', { name: 'New User' }).click();
    await expect(page.getByText('Create New User')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByText('Create New User')).not.toBeVisible({ timeout: 5_000 });
  });

  test('create user modal has required fields', async ({ page }) => {
    await gotoProtected(page, '/users');
    await page.getByRole('button', { name: 'New User' }).click();
    await expect(page.getByText('Create New User')).toBeVisible();
    await expect(page.locator('#createUser_username, [id*="username"]').first()).toBeVisible();
    await expect(page.locator('#createUser_email, [id*="email"]').first()).toBeVisible();
    await expect(page.locator('#createUser_password, [id*="password"]').first()).toBeVisible();
  });

  test('search filters the user table', async ({ page }) => {
    await gotoProtected(page, '/users');
    await expect(page.getByText('User Management')).toBeVisible({ timeout: 10_000 });
    await page.getByPlaceholder('Search by username or email').fill('nonexistent_xyz_user');
    await expect(page.getByText('No data').last()).toBeVisible({ timeout: 5_000 });
  });

  test('edit button opens edit modal with pre-filled data', async ({ page }) => {
    await gotoProtected(page, '/users');
    await expect(page.getByText('User Management')).toBeVisible({ timeout: 10_000 });
    // Find and click the first edit (pencil) icon button in the table
    const editBtn = page.locator('button').filter({ has: page.locator('[data-icon="edit"]') }).first();
    await editBtn.click();
    await expect(page.getByText('Edit User')).toBeVisible({ timeout: 5_000 });
    // Form should be pre-filled — check Email field (always present in edit form)
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('First Name')).toBeVisible();
  });
});

test.describe('Roles Page', () => {
  test('renders roles table', async ({ page }) => {
    await gotoProtected(page, '/roles');
    await expect(page.getByText('Role Management')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('button', { name: 'New Role' })).toBeVisible();
  });

  test('new role modal renders form fields', async ({ page }) => {
    await gotoProtected(page, '/roles');
    await page.getByRole('button', { name: 'New Role' }).click();
    await expect(page.getByText('Create New Role')).toBeVisible({ timeout: 5_000 });
    await expect(page.locator('input[id*="name"]').first()).toBeVisible();
  });

  test('edit button opens edit role modal', async ({ page }) => {
    await gotoProtected(page, '/roles');
    await expect(page.getByText('Role Management')).toBeVisible({ timeout: 10_000 });
    const editBtn = page.locator('button').filter({ has: page.locator('[data-icon="edit"]') }).first();
    await editBtn.click();
    await expect(page.getByText('Edit Role')).toBeVisible({ timeout: 5_000 });
  });
});

test.describe('Permissions Page', () => {
  test('renders permissions table', async ({ page }) => {
    await gotoProtected(page, '/permissions');
    await expect(page.getByText('Permission Management')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('button', { name: 'New Permission' })).toBeVisible();
  });

  test('new permission modal renders form fields', async ({ page }) => {
    await gotoProtected(page, '/permissions');
    await page.getByRole('button', { name: 'New Permission' }).click();
    await expect(page.getByText('Create Permission')).toBeVisible({ timeout: 5_000 });
  });
});

test.describe('Audit Logs Page', () => {
  test('renders audit table with filters', async ({ page }) => {
    await gotoProtected(page, '/audit');
    await expect(page.getByRole('heading', { name: 'Audit Logs' })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByPlaceholder('Search by action, user, resource')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Refresh' })).toBeVisible();
  });
});

test.describe('Sessions Page', () => {
  test('renders sessions page with my sessions header', async ({ page }) => {
    await gotoProtected(page, '/sessions');
    await expect(page.getByText('Session Management')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('button', { name: /Revoke All My Sessions/i })).toBeVisible({ timeout: 10_000 });
  });

  test('my sessions table columns are rendered', async ({ page }) => {
    await gotoProtected(page, '/sessions');
    await expect(page.getByText('Session Management')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole('columnheader', { name: 'Session ID' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'IP' })).toBeVisible();
  });
});

test.describe('Settings Page', () => {
  test('renders profile, change-password, and API info cards', async ({ page }) => {
    await gotoProtected(page, '/settings');
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('Your Profile')).toBeVisible();
    await expect(page.locator('.ant-card-head-title').filter({ hasText: 'Change Password' })).toBeVisible();
    await expect(page.getByText('API Information')).toBeVisible();
  });

  test('change password form has required fields', async ({ page }) => {
    await gotoProtected(page, '/settings');
    await expect(page.locator('.ant-card-head-title').filter({ hasText: 'Change Password' })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByPlaceholder('Current password')).toBeVisible();
    await expect(page.locator('[placeholder="New password"]')).toBeVisible();
    await expect(page.getByPlaceholder('Confirm new password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Change Password' })).toBeVisible();
  });

  test('change password shows strength indicator when typing', async ({ page }) => {
    await gotoProtected(page, '/settings');
    await expect(page.locator('[placeholder="New password"]')).toBeVisible({ timeout: 10_000 });
    await page.locator('[placeholder="New password"]').fill('V3ryStr0ng!Passw0rd');
    // Strength indicator text should appear
    await expect(page.getByText(/Strength:/i)).toBeVisible({ timeout: 5_000 });
  });

  test('change password shows error for wrong current password', async ({ page }) => {
    await gotoProtected(page, '/settings');
    await expect(page.getByPlaceholder('Current password')).toBeVisible({ timeout: 10_000 });
    await page.getByPlaceholder('Current password').fill('WrongOldPassword@1');
    await page.locator('[placeholder="New password"]').fill('V3ryStr0ng!Passw0rd#2');
    await page.getByPlaceholder('Confirm new password').fill('V3ryStr0ng!Passw0rd#2');
    await page.getByRole('button', { name: 'Change Password' }).click();
    await expect(page.getByText(/Failed to change password/i)).toBeVisible({ timeout: 8_000 });
  });
});
