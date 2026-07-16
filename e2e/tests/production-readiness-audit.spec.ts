import { expect, test, type Locator, type Page } from '@playwright/test';
import { seededStudent } from './support/auth-api.js';
import { assistantPage, enableFlutterSemantics, homePage, loginPage } from './support/flutter-semantics.js';

const auditDirectory = '../design-qa/production-audit';

async function enter(locator: Locator, value: string): Promise<void> {
  await locator.click();
  await locator.fill(value);
}

async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await enableFlutterSemantics(page);
  const login = loginPage(page);
  await enter(login.phone, seededStudent.phoneNumber);
  await enter(login.password, seededStudent.password);
  await login.loginButton.click();
  await expect(homePage(page).screen).toBeVisible();
  await expect(page.getByText('Nhữ Xuân Việt', { exact: true })).toBeVisible();
}

async function openNavigation(page: Page, label: string, expectedUrl: RegExp): Promise<void> {
  await page.getByRole('tab', { name: label, exact: true }).last().click();
  await expect(page).toHaveURL(expectedUrl);
  await page.mouse.move(1, 1);
  await page.waitForTimeout(250);
}

test('captures the production-critical Student journey', async ({ page }) => {
  test.setTimeout(60_000);
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page);
  await page.screenshot({ path: `${auditDirectory}/08-student-home.png`, animations: 'disabled' });

  await openNavigation(page, 'Lịch học', /#\/schedule$/);
  await page.screenshot({ path: `${auditDirectory}/09-student-timetable.png`, animations: 'disabled' });

  await openNavigation(page, 'Kết quả', /#\/grades$/);
  await page.screenshot({ path: `${auditDirectory}/10-student-grades.png`, animations: 'disabled' });

  await openNavigation(page, 'Hoạt động', /#\/events$/);
  await page.screenshot({ path: `${auditDirectory}/11-student-activities.png`, animations: 'disabled' });

  await openNavigation(page, 'Cá nhân', /#\/more$/);
  await expect(page.getByRole('group', { name: 'Trang cá nhân', exact: true })).toBeVisible();
  await page.screenshot({ path: `${auditDirectory}/12-student-profile.png`, animations: 'disabled' });

  await page.getByRole('button', { name: 'Mở trợ lý AI', exact: true }).click();
  await expect(assistantPage(page).screen).toBeVisible();
  await page.screenshot({ path: `${auditDirectory}/13-student-assistant.png`, animations: 'disabled' });
});
