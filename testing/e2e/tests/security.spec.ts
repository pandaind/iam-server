import { test, expect } from '@playwright/test';
import { API_URL, getAdminAuth } from './helpers';

let accessToken: string;
const testUsername = 'pw_sec_e2e_user';
let testUserId: number;

test.beforeAll(async ({ request }) => {
  ({ accessToken } = getAdminAuth());

  // Create a fresh test user for security tests
  const res = await request.post(`${API_URL}/users`, {
    headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
    data: {
      username: testUsername,
      email: 'pw_sec_e2e@example.com',
      password: 'SecTest@12345',
      firstName: 'Security',
      lastName: 'E2E',
    },
  });
  if (res.ok()) {
    testUserId = (await res.json()).id;
  }
});

test.afterAll(async ({ request }) => {
  if (testUserId) {
    await request.delete(`${API_URL}/users/${testUserId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }
});

test.describe('Password Policy API', () => {
  test('GET /security/password-policy returns policy rules', async ({ request }) => {
    const res = await request.get(`${API_URL}/security/password-policy`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('minLength');
    expect(body).toHaveProperty('requireUppercase');
    expect(body).toHaveProperty('requireLowercase');
    expect(body).toHaveProperty('requireNumbers');
    expect(body).toHaveProperty('requireSpecialChars');
    expect(typeof body.minLength).toBe('number');
    expect(body.minLength).toBeGreaterThan(0);
  });

  test('POST /security/validate-password — weak password fails', async ({ request }) => {
    const res = await request.post(`${API_URL}/security/validate-password`, {
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      data: { password: '123' },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.valid).toBe(false);
    expect(Array.isArray(body.errors)).toBe(true);
    expect(body.errors.length).toBeGreaterThan(0);
  });

  test('POST /security/validate-password — strong password passes', async ({ request }) => {
    const res = await request.post(`${API_URL}/security/validate-password`, {
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      data: { password: 'V3ryStr0ng!Passw0rd#' },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.valid).toBe(true);
    expect(body.errors).toHaveLength(0);
    expect(body).toHaveProperty('strength');
    expect(['GOOD', 'STRONG', 'VERY_STRONG']).toContain(body.strength);
  });

  test('POST /security/validate-password — returns strength rating', async ({ request }) => {
    const res = await request.post(`${API_URL}/security/validate-password`, {
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      data: { password: 'V3ryStr0ng!Passw0rd#' },
    });
    const body = await res.json();
    expect(['VERY_WEAK', 'WEAK', 'FAIR', 'GOOD', 'STRONG', 'VERY_STRONG']).toContain(body.strength);
  });
});

test.describe('Password Change API', () => {
  test('POST /security/change-password — wrong old password returns 400', async ({ request }) => {
    const res = await request.post(`${API_URL}/security/change-password`, {
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      data: { oldPassword: 'WrongOldPassword@1', newPassword: 'NewValid@Pass123' },
    });
    expect(res.status()).toBe(400);
  });

  test('POST /security/change-password — missing fields returns 400', async ({ request }) => {
    const res = await request.post(`${API_URL}/security/change-password`, {
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      data: {},
    });
    expect(res.status()).toBe(400);
  });
});

test.describe('Account Status & Management API', () => {
  test('GET /security/account-status/:username — active account is not locked', async ({ request }) => {
    test.skip(!testUserId, 'Requires test user');
    const res = await request.get(`${API_URL}/security/account-status/${testUsername}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('locked');
    expect(body.locked).toBe(false);
  });

  test('POST /security/force-password-reset/:username succeeds for admin', async ({ request }) => {
    test.skip(!testUserId, 'Requires test user');
    const res = await request.post(
      `${API_URL}/security/force-password-reset/${testUsername}`,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('message');
  });

  test('account lockout: 5 failed logins locks account', async ({ request }) => {
    test.skip(!testUserId, 'Requires test user');
    // Trigger 5 failed logins
    for (let i = 0; i < 5; i++) {
      await request.post(`${API_URL}/auth/login`, {
        data: { username: testUsername, password: 'WrongPass@9999' },
      });
    }
    const statusRes = await request.get(
      `${API_URL}/security/account-status/${testUsername}`,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    expect(statusRes.status()).toBe(200);
    const body = await statusRes.json();
    expect(body.locked).toBe(true);
  });

  test('POST /users/:id/unlock re-enables locked account', async ({ request }) => {
    test.skip(!testUserId, 'Requires test user');
    const res = await request.post(`${API_URL}/users/${testUserId}/unlock`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);

    const statusRes = await request.get(
      `${API_URL}/security/account-status/${testUsername}`,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    const body = await statusRes.json();
    expect(body.locked).toBe(false);
  });
});
