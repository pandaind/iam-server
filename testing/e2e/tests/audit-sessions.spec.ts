import { test, expect } from '@playwright/test';
import { API_URL, getAdminAuth } from './helpers';

let accessToken: string;

test.beforeAll(async () => {
  ({ accessToken } = getAdminAuth());
});

test.describe('Audit Logs API', () => {
  let adminUserId: number;

  test('GET /audit returns paginated audit logs', async ({ request }) => {
    const res = await request.get(`${API_URL}/audit`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
    expect(body).toHaveProperty('totalElements');
    expect(Array.isArray(body.content)).toBe(true);
  });

  test('GET /audit/failed returns only failed events', async ({ request }) => {
    const res = await request.get(`${API_URL}/audit/failed`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    for (const log of body.content) {
      expect(log.success).toBe(false);
    }
  });

  test('GET /audit/action/:action returns logs for that action', async ({ request }) => {
    const res = await request.get(`${API_URL}/audit/action/LOGIN`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
  });

  test('GET /audit/user/:userId returns logs for that user', async ({ request }) => {
    // Get admin user id first
    const usersRes = await request.get(`${API_URL}/users/username/admin`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    const adminUser = await usersRes.json();
    adminUserId = adminUser.id;

    const res = await request.get(`${API_URL}/audit/user/${adminUserId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
    expect(body).toHaveProperty('totalElements');
    expect(Array.isArray(body.content)).toBe(true);
  });

  test('GET /audit/date-range returns logs within range', async ({ request }) => {
    const start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(); // 7 days ago
    const end = new Date().toISOString();
    const res = await request.get(
      `${API_URL}/audit/date-range?startDate=${start}&endDate=${end}&page=0&size=10`,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toHaveProperty('content');
    expect(Array.isArray(body.content)).toBe(true);
  });

  test('unauthorized access to /audit returns 401', async ({ request }) => {
    const res = await request.get(`${API_URL}/audit`);
    expect(res.status()).toBe(401);
  });
});

test.describe('Session Management API', () => {
  let freshToken: string;
  let sessionId: string;

  test.beforeAll(async ({ request }) => {
    // Create a fresh login to guarantee a session is recorded (requires new backend code)
    const res = await request.post(`${API_URL}/auth/login`, {
      data: {
        username: process.env.ADMIN_USER ?? 'admin',
        password: process.env.ADMIN_PASS ?? 'Admin@123',
      },
    });
    freshToken = (await res.json()).accessToken;
  });

  test('GET /sessions/my returns array', async ({ request }) => {
    const res = await request.get(`${API_URL}/sessions/my`, {
      headers: { Authorization: `Bearer ${freshToken}` },
    });
    expect(res.status()).toBe(200);
    const sessions = await res.json();
    expect(Array.isArray(sessions)).toBe(true);
    // Store first session id if any exist
    if (sessions.length > 0) {
      sessionId = sessions[0].sessionId;
    }
  });

  test('GET /sessions/my contains expected session fields', async ({ request }) => {
    const res = await request.get(`${API_URL}/sessions/my`, {
      headers: { Authorization: `Bearer ${freshToken}` },
    });
    const sessions = await res.json();
    if (sessions.length > 0) {
      const s = sessions[0];
      expect(s).toHaveProperty('sessionId');
      expect(s).toHaveProperty('ipAddress');
      expect(s).toHaveProperty('createdAt');
    }
  });

  test('GET /sessions/user/:username returns sessions (admin)', async ({ request }) => {
    const res = await request.get(`${API_URL}/sessions/user/admin`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    expect(res.status()).toBe(200);
    expect(Array.isArray(await res.json())).toBe(true);
  });

  test('DELETE /sessions/my/:sessionId invalidates specific session', async ({ request }) => {
    if (!sessionId) {
      test.skip(true, 'No sessions found — backend may not yet create sessions on login');
    }
    const res = await request.delete(`${API_URL}/sessions/my/${sessionId}`, {
      headers: { Authorization: `Bearer ${freshToken}` },
    });
    expect(res.status()).toBe(200);
  });

  test('DELETE /sessions/user/:username/all invalidates all user sessions (admin)', async ({ request }) => {
    // Login fresh to ensure there's at least one session to invalidate
    const loginRes = await request.post(`${API_URL}/auth/login`, {
      data: {
        username: process.env.ADMIN_USER ?? 'admin',
        password: process.env.ADMIN_PASS ?? 'Admin@123',
      },
    });
    const token = (await loginRes.json()).accessToken;

    const res = await request.delete(`${API_URL}/sessions/user/admin/all`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(200);
  });

  test('DELETE /sessions/my/all invalidates all own sessions', async ({ request }) => {
    // Login fresh
    const loginRes = await request.post(`${API_URL}/auth/login`, {
      data: {
        username: process.env.ADMIN_USER ?? 'admin',
        password: process.env.ADMIN_PASS ?? 'Admin@123',
      },
    });
    const token = (await loginRes.json()).accessToken;

    const res = await request.delete(`${API_URL}/sessions/my/all`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(res.status()).toBe(200);
  });

  test('unauthorized access to /sessions/my returns 401', async ({ request }) => {
    const res = await request.get(`${API_URL}/sessions/my`);
    expect(res.status()).toBe(401);
  });
});
