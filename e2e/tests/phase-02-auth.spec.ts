import { expect, test, type Locator, type Page } from '@playwright/test';

import {
  AuthApi,
  expectTokenResponse,
  expectUnauthorizedProblem,
  seededStudent,
} from './support/auth-api.js';
import {
  enableFlutterSemantics,
  homePage,
  loginPage,
  resetPasswordPage,
} from './support/flutter-semantics.js';

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080';
let restorePasswordAfterFailure = false;

async function enterFlutterText(field: Locator, value: string): Promise<void> {
  await field.click();
  await field.press('ControlOrMeta+A');
  await field.pressSequentially(value, { delay: 20 });
  await expect(field).toHaveValue(value);
}

async function loginThroughUi(page: Page, password: string): Promise<void> {
  const login = loginPage(page);
  await enterFlutterText(login.phone, seededStudent.phoneNumber);
  await enterFlutterText(login.password, password);
  await login.loginButton.click();
  await expect(homePage(page).screen).toBeVisible();
  await expect(page).toHaveURL(/\/home$/);
}

async function logoutThroughUi(page: Page): Promise<void> {
  await homePage(page).profileButton.click();
  await expect(page).toHaveURL(/\/more$/);
  await homePage(page).logoutButton.click();
  await expect(loginPage(page).screen).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
}

async function resetPasswordThroughUi(
  page: Page,
  newPassword: string,
): Promise<void> {
  await loginPage(page).forgotPasswordButton.click();
  const reset = resetPasswordPage(page);
  await expect(reset.heading).toBeVisible();

  await enterFlutterText(reset.phone, seededStudent.phoneNumber);
  await reset.requestOtpButton.click();
  await expect(reset.otpDigits).toHaveCount(6);
  for (const [index, digit] of [...seededStudent.otp].entries()) {
    const field = reset.otpDigits.nth(index);
    await field.click();
    await field.press(digit);
    await expect(field).toHaveValue(digit);
  }
  await reset.verifyOtpButton.click();
  await expect(reset.newPassword).toBeVisible();

  await enterFlutterText(reset.newPassword, newPassword);
  await enterFlutterText(reset.confirmPassword, newPassword);
  await reset.completeButton.click();

  await expect(reset.backToLoginButton).toBeVisible();
  await reset.backToLoginButton.click();
  await expect(loginPage(page).screen).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
}

test.describe.serial('Phase 02 authentication', () => {
  test.afterAll(async ({ request }) => {
    if (!restorePasswordAfterFailure) return;

    const auth = new AuthApi(request, apiBaseUrl);
    await auth.resetPassword(seededStudent.password);
  });

  test('rotates refresh tokens, rejects replay, and revokes logout sessions', async ({
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);

    const logoutLogin = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const logoutRotation = await expectTokenResponse(
      await auth.refresh(logoutLogin.refreshToken),
    );
    expect(logoutRotation.refreshToken).not.toBe(logoutLogin.refreshToken);
    expect(logoutRotation.accessToken).not.toBe(logoutLogin.accessToken);

    expect((await auth.logout(logoutRotation.refreshToken)).status()).toBe(204);
    expect((await auth.logout(logoutRotation.refreshToken)).status()).toBe(204);
    await expectUnauthorizedProblem(
      await auth.refresh(logoutRotation.refreshToken),
    );

    const replayLogin = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const replayRotation = await expectTokenResponse(
      await auth.refresh(replayLogin.refreshToken),
    );
    expect(replayRotation.refreshToken).not.toBe(replayLogin.refreshToken);

    await expectUnauthorizedProblem(await auth.refresh(replayLogin.refreshToken));
    await expectUnauthorizedProblem(
      await auth.refresh(replayRotation.refreshToken),
    );
  });

  test('logs the seeded student in through Flutter and logs out', async ({
    page,
  }) => {
    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginThroughUi(page, seededStudent.password);
    await logoutThroughUi(page);
  });

  test('resets the password through Flutter and restores the seed password', async ({
    page,
  }) => {
    restorePasswordAfterFailure = true;
    await page.goto('/login');
    await enableFlutterSemantics(page);

    await resetPasswordThroughUi(page, seededStudent.replacementPassword);
    await loginThroughUi(page, seededStudent.replacementPassword);
    await logoutThroughUi(page);

    await resetPasswordThroughUi(page, seededStudent.password);
    await loginThroughUi(page, seededStudent.password);
    await logoutThroughUi(page);
    restorePasswordAfterFailure = false;
  });
});
