import { test as setup } from '@playwright/test';
import fs from 'fs';
import path from 'path';

/**
 * Global setup: obtain admin JWT and store auth state for reuse across tests.
 */
setup('authenticate as admin', async ({ request }) => {
  const apiUrl = process.env.API_URL ?? 'http://localhost:8080/api/v1';
  const username = process.env.ADMIN_USER ?? 'admin';
  const password = process.env.ADMIN_PASS ?? 'Admin@123';

  const response = await request.post(`${apiUrl}/auth/login`, {
    data: { username, password },
  });

  if (!response.ok()) {
    throw new Error(
      `Admin login failed: ${response.status()} ${await response.text()}`,
    );
  }

  const body = await response.json();
  const authState = {
    accessToken: body.accessToken,
    refreshToken: body.refreshToken,
    user: body.user,
  };

  const dir = path.join(__dirname, '..', '.auth');
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, 'admin.json'), JSON.stringify(authState, null, 2));
});
