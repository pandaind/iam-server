import { test, expect, request } from '@playwright/test';
import { API_URL } from './helpers';

const ACTUATOR_URL = process.env.ACTUATOR_URL ?? `${API_URL}/actuator`;

const adminCreds = {
  username: process.env.ADMIN_USER ?? 'admin',
  password: process.env.ADMIN_PASS ?? 'Admin@123',
};

test.describe('Authentication API', () => {
  test('health endpoint returns UP', async ({ request: req }) => {
    const res = await req.get(`${ACTUATOR_URL}/health`);
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.status).toBe('UP');
  });

  test('public health endpoint returns status', async ({ request: req }) => {
    const res = await req.get(`${API_URL}/public/health`);
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('status');
  });

  test('login with valid credentials returns tokens', async ({ request: req }) => {
    const res = await req.post(`${API_URL}/auth/login`, { data: adminCreds });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('accessToken');
    expect(body).toHaveProperty('refreshToken');
    expect(body.tokenType).toBe('Bearer');
    expect(body.accessToken).toBeTruthy();
  });

  test('login with wrong password returns 401', async ({ request: req }) => {
    const res = await req.post(`${API_URL}/auth/login`, {
      data: { username: adminCreds.username, password: 'wrongpassword' },
    });
    expect(res.status()).toBe(401);
  });

  test('login with missing fields returns 400', async ({ request: req }) => {
    const res = await req.post(`${API_URL}/auth/login`, { data: {} });
    expect(res.status()).toBe(400);
  });

  test('accessing protected endpoint without token returns 401', async ({ request: req }) => {
    const res = await req.get(`${API_URL}/users`);
    expect(res.status()).toBe(401);
  });

  test('token refresh returns new access token', async ({ request: req }) => {
    // First login
    const loginRes = await req.post(`${API_URL}/auth/login`, { data: adminCreds });
    const { refreshToken } = await loginRes.json();

    const refreshRes = await req.post(`${API_URL}/auth/refresh?refreshToken=${refreshToken}`);
    expect(refreshRes.status()).toBe(200);
    const body = await refreshRes.json();
    expect(body).toHaveProperty('accessToken');
  });

  test('POST /auth/logout returns 200 and token is blacklisted', async ({ request: req }) => {
    // Login to get a fresh token
    const loginRes = await req.post(`${API_URL}/auth/login`, { data: adminCreds });
    expect(loginRes.status()).toBe(200);
    const { accessToken } = await loginRes.json();

    // Logout with that token
    const logoutRes = await req.post(`${API_URL}/auth/logout`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(logoutRes.status()).toBe(200);

    // The blacklisted token must now be rejected
    const protectedRes = await req.get(`${API_URL}/users`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(protectedRes.status()).toBe(401);
  });
});

test.describe('Public Endpoints', () => {
  test('GET /public/capabilities returns feature flags', async ({ request: req }) => {
    const res = await req.get(`${API_URL}/public/capabilities`);
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('authentication');
    expect(body).toHaveProperty('authorization');
    expect(body).toHaveProperty('security');
    expect(body).toHaveProperty('oauth2Endpoints');
    expect(body.authentication.jwt).toBe(true);
    expect(body.authorization.rbac).toBe(true);
    expect(body.security.accountLocking).toBe(true);
    expect(body.security.auditLogging).toBe(true);
    expect(body.oauth2Endpoints).toHaveProperty('authorization');
    expect(body.oauth2Endpoints).toHaveProperty('token');
  });
});
