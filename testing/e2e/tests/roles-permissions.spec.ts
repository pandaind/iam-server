import { test, expect } from '@playwright/test';
import { API_URL, getAdminAuth } from './helpers';

let accessToken: string;

test.beforeAll(async () => {
  ({ accessToken } = getAdminAuth());
});

test.describe('Role Management API', () => {
  let createdRoleId: number;

  test('GET /roles returns paginated list', async ({ request }) => {
    const res = await request.get(`${API_URL}/roles`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
    expect(Array.isArray(body.content)).toBe(true);
  });

  test('GET /roles/active returns array of active roles', async ({ request }) => {
    const res = await request.get(`${API_URL}/roles/active`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    expect(Array.isArray(await res.json())).toBe(true);
  });

  test('POST /roles creates a role', async ({ request }) => {
    const res = await request.post(`${API_URL}/roles`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      data: { name: 'ROLE_PW_E2E_TEST', description: 'Playwright E2E test role' },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body).toHaveProperty('id');
    expect(body.name).toBe('ROLE_PW_E2E_TEST');
    createdRoleId = body.id;
  });

  test('GET /roles/:id returns the role', async ({ request }) => {
    test.skip(!createdRoleId, 'Depends on role creation test');
    const res = await request.get(`${API_URL}/roles/${createdRoleId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.id).toBe(createdRoleId);
  });

  test('POST /roles/:id/deactivate deactivates the role', async ({ request }) => {
    test.skip(!createdRoleId, 'Depends on role creation test');
    const res = await request.post(`${API_URL}/roles/${createdRoleId}/deactivate`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
  });

  test('POST /roles/:id/activate re-activates the role', async ({ request }) => {
    test.skip(!createdRoleId, 'Depends on role creation test');
    const res = await request.post(`${API_URL}/roles/${createdRoleId}/activate`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
  });

  test('DELETE /roles/:id removes the role', async ({ request }) => {
    test.skip(!createdRoleId, 'Depends on role creation test');
    const res = await request.delete(`${API_URL}/roles/${createdRoleId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(204);
  });
});

test.describe('Permission Management API', () => {
  let createdPermId: number;

  test('GET /permissions returns paginated list', async ({ request }) => {
    const res = await request.get(`${API_URL}/permissions`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
  });

  test('GET /permissions/all returns full list', async ({ request }) => {
    const res = await request.get(`${API_URL}/permissions/all`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    expect(Array.isArray(await res.json())).toBe(true);
  });

  test('POST /permissions creates a permission', async ({ request }) => {
    const res = await request.post(
      `${API_URL}/permissions?name=PW_E2E_PERM&description=E2E+test&resource=test&action=read`,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body).toHaveProperty('id');
    createdPermId = body.id;
  });

  test('DELETE /permissions/:id removes the permission', async ({ request }) => {
    test.skip(!createdPermId, 'Depends on permission creation test');
    const res = await request.delete(`${API_URL}/permissions/${createdPermId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(204);
  });
});
