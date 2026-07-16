import { expect, test, type Page } from '@playwright/test';

const auditDirectory = '../design-qa/production-audit';

async function login(page: Page): Promise<void> {
  await page.context().clearCookies();
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible();
  await page.screenshot({
    path: `${auditDirectory}/01-admin-login.png`,
    animations: 'disabled',
  });
  await page.getByLabel('Số điện thoại').fill('0900000000');
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill('Admin@123');
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();
  await expect(page).toHaveURL(new RegExp('/$'));
}

async function captureRoute(
  page: Page,
  path: string,
  heading: string,
  fileName: string,
): Promise<void> {
  await page.goto(path);
  await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
  await expect(page.getByRole('progressbar')).toHaveCount(0);
  await page.waitForTimeout(250);
  await page.screenshot({
    path: `${auditDirectory}/${fileName}`,
    animations: 'disabled',
  });
}

test('captures the current production-critical Admin journey', async ({ page }) => {
  test.setTimeout(60_000);
  await page.setViewportSize({ width: 1440, height: 900 });
  await login(page);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Chào buổi/ })).toBeVisible();
  await page.screenshot({
    path: `${auditDirectory}/02-admin-dashboard.png`,
    animations: 'disabled',
  });
  await captureRoute(page, '/students', 'Học sinh', '03-admin-students.png');
  await captureRoute(page, '/timetable', 'Lịch học', '04-admin-timetable.png');
  await page.goto('/grades');
  await expect(page.getByRole('heading', { name: 'Điểm số', exact: true })).toBeVisible();
  await page.getByRole('combobox', { name: 'Học sinh', exact: true }).click();
  await page.getByRole('option', { name: /Nhữ Xuân Việt/ }).click();
  await expect(page.getByText('Toán', { exact: true })).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('listbox')).toHaveCount(0);
  await page.waitForTimeout(250);
  await page.screenshot({
    path: `${auditDirectory}/05-admin-grades.png`,
    animations: 'disabled',
  });
  await captureRoute(page, '/activities', 'Sự kiện & CLB', '06-admin-activities.png');
  await captureRoute(page, '/ai-settings', 'Cấu hình AI', '07-admin-ai-settings.png');
});
