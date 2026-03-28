import { test, expect } from '@playwright/test';
import { API_URL, getAdminAuth } from './helpers';

let accessToken: string;

test.beforeAll(async () => {
  ({ accessToken } = getAdminAuth());
});

test.describe('User Management API', () => {
  let createdUserId: number;

  test('GET /users returns paginated list', async ({ request }) => {
    const res = await request.get(`${API_URL}/users`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
    expect(body).toHaveProperty('totalElements');
    expect(Array.isArray(body.content)).toBe(true);
  });

  test('POST /users creates a new user', async ({ request }) => {
    const res = await request.post(`${API_URL}/users`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      data: {
        username: 'pw_e2e_testuser',
        email: 'pw_e2e_testuser@example.com',
        password: 'Test@12345',
        firstName: 'Playwright',
        lastName: 'E2E',
      },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body).toHaveProperty('id');
    expect(body.username).toBe('pw_e2e_testuser');
    createdUserId = body.id;
  });

  test('GET /users/:id returns the created user', async ({ request }) => {
    test.skip(!createdUserId, 'Depends on user creation test');
    const res = await request.get(`${API_URL}/users/${createdUserId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.id).toBe(createdUserId);
    expect(body.username).toBe('pw_e2e_testuser');
  });

  test('PUT /users/:id updates the user', async ({ request }) => {
    test.skip(!createdUserId, 'Depends on user creation test');
    const res = await request.put(`${API_URL}/users/${createdUserId}`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      data: {
        username: 'pw_e2e_testuser',
        firstName: 'Updated',
        lastName: 'E2E',
        email: 'pw_e2e_testuser@example.com',
      },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.firstName).toBe('Updated');
  });

  test('POST /users/:id/disable disables the user', async ({ request }) => {
    test.skip(!createdUserId, 'Depends on user creation test');
    const res = await request.post(`${API_URL}/users/${createdUserId}/disable`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
  });

  test('POST /users/:id/enable re-enables the user', async ({ request }) => {
    test.skip(!createdUserId, 'Depends on user creation test');
    const res = await request.post(`${API_URL}/users/${createdUserId}/enable`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
  });

  test('DELETE /users/:id removes the user', async ({ request }) => {
    test.skip(!createdUserId, 'Depends on user creation test');
    const res = await request.delete(`${API_URL}/users/${createdUserId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(204);
  });

  test('GET /users/:id after delete returns 404', async ({ request }) => {
    test.skip(!createdUserId, 'Depends on user creation test');
    const res = await request.get(`${API_URL}/users/${createdUserId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(404);
  });
});

test.describe('User Lookup API', () => {
  test('GET /users/username/:username returns user by username', async ({ request }) => {
    const res = await request.get(`${API_URL}/users/username/admin`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.username).toBe('admin');
    expect(body).toHaveProperty('id');
    expect(body).toHaveProperty('email');
  });

  test('GET /users/username/:username — unknown user returns 404', async ({ request }) => {
    const res = await request.get(`${API_URL}/users/username/no_such_user_xyz`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(404);
  });

  test('GET /users/role/:roleName returns users with that role', async ({ request }) => {
    const res = await request.get(`${API_URL}/users/role/ADMIN`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThan(0);
    for (const user of body) {
      expect(user.roles).toContain('ADMIN');
    }
  });
});
