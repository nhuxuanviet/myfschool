import { expect, test, type Locator, type Page } from '@playwright/test';

import {
  AuthApi,
  expectTokenResponse,
  seededStudent,
} from './support/auth-api.js';
import {
  enableFlutterSemantics,
  homePage,
  loginPage,
  timetablePage,
} from './support/flutter-semantics.js';
import {
  addDays,
  expectTimetableResponse,
  expectTimetableUnauthorized,
  findScheduledTimetableLesson,
  findTimetableException,
  TimetableApi,
} from './support/timetable-api.js';

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

test.describe('Phase 04 Vietnamese secondary-school timetable', () => {
  test('requires authentication and returns configured 45-minute timetable weeks', async ({
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const timetable = new TimetableApi(request, apiBaseUrl);

    await expectTimetableUnauthorized(await timetable.getUnauthenticated());

    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const currentWeek = await expectTimetableResponse(
      await timetable.get(tokenResponse.accessToken),
    );
    expect(currentWeek.academicTerms).not.toHaveLength(0);
    const exception = findTimetableException(currentWeek);
    expect(exception.date).toMatch(/^\d{4}-\d{2}-\d{2}$/);

    const nextWeekStart = addDays(currentWeek.weekStart, 7);
    const nextWeek = await expectTimetableResponse(
      await timetable.get(tokenResponse.accessToken, nextWeekStart),
    );
    expect(nextWeek.weekStart).toBe(nextWeekStart);
    expect(nextWeek.weekEnd).toBe(addDays(nextWeekStart, 6));

    const previousWeekStart = addDays(currentWeek.weekStart, -7);
    const previousWeek = await expectTimetableResponse(
      await timetable.get(tokenResponse.accessToken, previousWeekStart),
    );
    expect(previousWeek.weekStart).toBe(previousWeekStart);
    expect(previousWeek.weekEnd).toBe(addDays(previousWeekStart, 6));
  });

  test('lets the student change day and week, including a schedule exception', async ({
    page,
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const timetableApi = new TimetableApi(request, apiBaseUrl);
    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const currentWeek = await expectTimetableResponse(
      await timetableApi.get(tokenResponse.accessToken),
    );
    const scheduledLesson = findScheduledTimetableLesson(currentWeek);
    const exception = findTimetableException(currentWeek);

    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginThroughUi(page);

    await homePage(page).quickAction('Lịch học').click();
    await expect(page).toHaveURL(/\/schedule$/);

    const schedule = timetablePage(page);
    await expect(schedule.screen).toBeVisible();
    await expect(schedule.weekRange(currentWeek.weekStart, currentWeek.weekEnd)).toBeVisible();

    await page.getByRole('button', { name: 'Chọn ngày', exact: true }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Chọn năm tháng 7 năm 2026', exact: true }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Hủy', exact: true }).click();

    await schedule.dayButton(scheduledLesson.date).click();
    await expect(schedule.selectedDay(scheduledLesson.date)).toBeVisible();
    await expect(schedule.lesson(scheduledLesson)).toBeVisible();

    await schedule.dayButton(exception.date).click();
    await expect(schedule.selectedDay(exception.date)).toBeVisible();
    await expect(schedule.lesson(exception)).toBeVisible();
    await expect(schedule.exceptionLesson(exception)).toBeVisible();

    const nextWeekStart = addDays(currentWeek.weekStart, 7);
    await schedule.nextWeekButton.click();
    await expect(schedule.weekRange(nextWeekStart, addDays(nextWeekStart, 6))).toBeVisible();

    await schedule.previousWeekButton.click();
    await expect(schedule.weekRange(currentWeek.weekStart, currentWeek.weekEnd)).toBeVisible();

    await page.getByRole('button', { name: 'Quay lại trang chủ', exact: true }).click();
    await expect(page).toHaveURL(/\/home$/);
    await expect(homePage(page).screen).toBeVisible();
  });
});
