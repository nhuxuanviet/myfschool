import { expect, test, type Locator, type Page } from '@playwright/test';

import { AuthApi, expectTokenResponse, seededStudent } from './support/auth-api.js';
import {
  assistantPage,
  enableFlutterSemantics,
  homePage,
  loginPage,
} from './support/flutter-semantics.js';

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080';

test.setTimeout(120_000);
test.skip(
  process.env.E2E_EXPECT_OPENAI !== 'true',
  'Runs only against an explicitly configured OpenAI-backed dev server.',
);

async function enter(field: Locator, value: string): Promise<void> {
  await field.click();
  await field.press('ControlOrMeta+A');
  await field.pressSequentially(value);
}

async function loginUi(page: Page): Promise<void> {
  const login = loginPage(page);
  await enter(login.phone, seededStudent.phoneNumber);
  await enter(login.password, seededStudent.password);
  await login.loginButton.click();
  await expect(homePage(page).screen).toBeVisible();
}

test('uses OpenAI to answer one focused grade question through API and Flutter streaming', async ({
  page,
  request,
}) => {
  const auth = new AuthApi(request, apiBaseUrl);
  const session = await expectTokenResponse(await auth.login(seededStudent.password));
  const response = await request.post(`${apiBaseUrl}/api/v1/assistant/chat`, {
    headers: { Authorization: `Bearer ${session.accessToken}` },
    data: {
      message: 'Có điểm Toán chưa?',
      conversationId: 'openai-playwright-api',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json() as { answer: string; mode: string };
  expect(body.mode).toBe('OPENAI');
  expect(body.answer).toContain('8,7');
  expect(body.answer).not.toMatch(/Ngữ văn|Giáo dục thể chất|ACHIEVED/);

  await page.goto('/login');
  await enableFlutterSemantics(page);
  await loginUi(page);
  await page.getByRole('button', { name: 'Mở trợ lý AI', exact: true }).click();

  const assistant = assistantPage(page);
  await enter(assistant.question, 'Có điểm Toán chưa?');
  await assistant.sendButton.click();
  await expect(assistant.answer(/8,7/)).toBeVisible({ timeout: 60_000 });
  await expect(assistant.answer(/Ngữ văn|Giáo dục thể chất|ACHIEVED/)).toHaveCount(0);
  await expect(assistant.sendButton).toBeEnabled();
});
