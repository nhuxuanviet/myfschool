import { expect, test, type Locator, type Page } from '@playwright/test';

import {
  AuthApi,
  expectTokenResponse,
  seededPeerStudent,
  seededStudent,
} from './support/auth-api.js';
import {
  expectFormDetails,
  expectFormNotFound,
  expectFormsResponse,
  StudentFormsApi,
} from './support/forms-api.js';
import {
  enableFlutterSemantics,
  formsPage,
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
}

test.describe('Phase 07 student forms and approval timeline', () => {
  test('lists only owned forms, filters statuses, and protects peer details', async ({
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const formsApi = new StudentFormsApi(request, apiBaseUrl);
    expect((await formsApi.listUnauthenticated()).status()).toBe(401);

    const student = await expectTokenResponse(await auth.login(seededStudent.password));
    const peer = await expectTokenResponse(
      await auth.loginWithCredentials(seededPeerStudent),
    );
    const forms = await expectFormsResponse(await formsApi.list(student.accessToken));
    const peerForms = await expectFormsResponse(await formsApi.list(peer.accessToken));
    expect(forms.forms.length).toBeGreaterThan(0);
    expect(peerForms.forms.length).toBeGreaterThan(0);

    const injected = await expectFormsResponse(
      await formsApi.listWithStudentId(student.accessToken, peer.student.id),
    );
    expect(injected).toEqual(forms);

    const peerOnly = peerForms.forms.find(
      (form) => !forms.forms.some((candidate) => candidate.id === form.id),
    );
    if (peerOnly === undefined) throw new Error('Expected a peer-owned seeded form.');
    await expectFormNotFound(
      await formsApi.get(student.accessToken, peerOnly.id),
      peerOnly.id,
    );

    for (const status of [...new Set(forms.forms.map((form) => form.status))]) {
      const filtered = await expectFormsResponse(
        await formsApi.list(student.accessToken, status),
      );
      expect(filtered.forms.every((form) => form.status === status)).toBe(true);
    }

    const approved = forms.forms.find((form) => form.status === 'APPROVED');
    if (approved === undefined) throw new Error('Expected an approved seeded form.');
    const details = await expectFormDetails(
      await formsApi.get(student.accessToken, approved.id),
    );
    expect(details.timeline.map((entry) => entry.status)).toEqual([
      'SUBMITTED',
      'IN_REVIEW',
      'APPROVED',
    ]);
  });

  test('creates and cancels a form through the API', async ({ request }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const formsApi = new StudentFormsApi(request, apiBaseUrl);
    const student = await expectTokenResponse(await auth.login(seededStudent.password));

    const created = await expectFormDetails(
      await formsApi.create(student.accessToken, {
        type: 'STUDENT_CONFIRMATION',
        reason: 'Xin giấy xác nhận học sinh cho kiểm thử E2E.',
      }),
      201,
    );
    expect(created.status).toBe('SUBMITTED');
    expect(created.canCancel).toBe(true);

    const cancelled = await expectFormDetails(
      await formsApi.cancel(student.accessToken, created.id),
    );
    expect(cancelled.status).toBe('CANCELLED');
    expect(cancelled.canCancel).toBe(false);
    expect(cancelled.timeline.at(-1)?.status).toBe('CANCELLED');
  });

  test('creates, opens, cancels, and reloads a form through Flutter', async ({
    page,
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const formsApi = new StudentFormsApi(request, apiBaseUrl);
    const student = await expectTokenResponse(await auth.login(seededStudent.password));

    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginThroughUi(page);
    await homePage(page).profileButton.click();
    await page.getByRole('button', { name: 'Đơn từ của tôi', exact: true }).click();

    const forms = formsPage(page);
    await expect(forms.screen).toBeVisible();
    await forms.createButton.click();
    await forms.formTypeButton.click();
    await forms.formTypeOption('STUDENT_CONFIRMATION').click();
    await enterFlutterText(
      forms.reason,
      'Xin giấy xác nhận học sinh từ ứng dụng Flutter.',
    );
    await forms.submitButton.click();

    await expect(page).toHaveURL((url) => /^#\/forms\/[0-9a-f-]{36}$/.test(url.hash));
    const formId = new URL(page.url()).hash.split('/').at(-1);
    if (formId === undefined) throw new Error('Created form ID was not present in the URL.');
    const created = await expectFormDetails(
      await formsApi.get(student.accessToken, formId),
    );
    await expect(forms.detailScreen(created)).toBeVisible();
    await forms.cancelButton.click();
    await forms.confirmCancelButton.click();
    await expect(forms.statusText('Đã hủy')).toBeVisible();

    const cancelled = await expectFormDetails(
      await formsApi.get(student.accessToken, formId),
    );
    expect(cancelled.status).toBe('CANCELLED');

    await page.reload();
    await enableFlutterSemantics(page, formsPage(page).detailScreen(cancelled));
    await expect(formsPage(page).statusText('Đã hủy')).toBeVisible();
  });
});
