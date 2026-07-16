import { expect, test, type Locator, type Page } from '@playwright/test';

import {
  AuthApi,
  expectHomeDashboardResponse,
  expectTokenResponse,
  seededStudent,
} from './support/auth-api.js';
import {
  enableFlutterSemantics,
  homePage,
  loginPage,
} from './support/flutter-semantics.js';

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080';

async function enterFlutterText(field: Locator, value: string): Promise<void> {
  await field.click();
  await field.press('ControlOrMeta+A');
  await field.pressSequentially(value);
  await expect(field).toHaveValue(value);
}

async function loginThroughUi(page: Page): Promise<void> {
  const login = loginPage(page);
  await enterFlutterText(login.phone, seededStudent.phoneNumber);
  await enterFlutterText(login.password, seededStudent.password);
  await login.loginButton.click();
  await expect(homePage(page).screen).toBeVisible();
  await expect(page).toHaveURL(/\/home$/);
}

test.describe('Phase 03 homepage', () => {
  test('requires a student token and returns the authenticated dashboard contract', async ({
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);

    expect((await request.get(`${apiBaseUrl}/api/v1/home`)).status()).toBe(401);
    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const dashboard = await expectHomeDashboardResponse(
      await auth.getHome(tokenResponse.accessToken),
    );

    expect(dashboard.student.studentCode).toBe(tokenResponse.student.studentCode);
    expect(dashboard.student.fullName).toBe(tokenResponse.student.fullName);
  });

  test('renders the real dashboard and opens an authenticated feature route', async ({
    page,
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const dashboard = await expectHomeDashboardResponse(
      await auth.getHome(tokenResponse.accessToken),
    );

    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginThroughUi(page);

    const home = homePage(page);
    await expect(
      home.studentProfile(
        dashboard.student.fullName,
        dashboard.student.className,
        dashboard.student.gradeLevel,
        dashboard.student.studentCode,
      ),
    ).toBeVisible();
    await expect(home.quickAction('Kết quả')).toBeVisible();
    await home.quickAction('Kết quả').click();
    await expect(page).toHaveURL(/\/grades$/);
    await expect(page.getByLabel('Điểm học kỳ', { exact: true })).toBeVisible();
  });

  test('opens student notifications from the separate Home bell action', async ({ page }) => {
    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginThroughUi(page);

    await page.getByRole('button', { name: 'Thông báo', exact: true }).click();
    await expect(page).toHaveURL(/\/notifications$/);
    await expect(
      page.getByRole('group', { name: 'Thông báo học sinh', exact: true }),
    ).toBeVisible();
  });
});
