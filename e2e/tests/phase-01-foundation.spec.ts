import { expect, test } from '@playwright/test';

import {
  enableFlutterSemantics,
  loginPage,
  resetPasswordPage,
} from './support/flutter-semantics.js';

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080';

test.describe('Phase 01 foundation', () => {
  test('serves the real backend health API', async ({ request }) => {
    const response = await request.get(`${apiBaseUrl}/api/v1/system/health`);

    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('application/json');
    const health = await response.json();
    expect(health).toEqual(
      expect.objectContaining({
        status: 'UP',
        timestamp: expect.any(String),
      }),
    );
  });

  test('exposes the Flutter login semantics and opens reset password', async ({
    page,
  }) => {
    await page.goto('/');
    await enableFlutterSemantics(page);

    const login = loginPage(page);
    await expect(login.screen).toBeVisible();
    await expect(login.logo).toBeVisible();
    await expect(login.phone).toBeVisible();
    await expect(login.password).toBeVisible();
    await expect(login.loginButton).toBeVisible();
    await expect(login.systemOnline).toBeVisible();

    await login.forgotPasswordButton.click();

    const reset = resetPasswordPage(page);
    await expect(reset.heading).toBeVisible();
    await expect(reset.phone).toBeVisible();
    await expect(reset.requestOtpButton).toBeVisible();

    await reset.backToLoginButton.click();
    await expect(loginPage(page).screen).toBeVisible();
  });
});
