import { expect, test, type Page } from '@playwright/test';

const adminPhone = '0900000000';
const adminPassword = 'Admin@123';

async function login(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Số điện thoại').fill(adminPhone);
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill(adminPassword);
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();
  await expect(page).toHaveURL(/\/$/);
}

test('renders live metrics and working dashboard controls', async ({ page }) => {
  const browserErrors: string[] = [];
  page.on('console', (message) => { if (message.type() === 'error') browserErrors.push(message.text()); });
  await login(page);

  await expect(page.getByText('Dữ liệu thời gian thực')).toBeVisible();
  for (const label of ['Học sinh', 'Lớp học', 'Đơn chờ xử lý', 'Sự kiện sắp tới', 'Đăng ký CLB', 'Điểm vừa cập nhật']) {
    await expect(page.getByRole('main').getByText(label, { exact: true })).toBeVisible();
  }

  await page.getByRole('button', { name: 'Mở thông báo' }).click();
  await expect(page.getByText('Thông báo mới')).toBeVisible();
  await page.keyboard.press('Escape');

  await page.getByRole('button', { name: 'Thu gọn menu' }).click();
  await expect(page.getByRole('button', { name: 'Mở rộng menu' })).toBeVisible();
  await page.getByRole('button', { name: 'Mở rộng menu' }).click();
  expect(browserErrors).toEqual([]);
});

test('opens every sidebar destination with a valid heading', async ({ page }) => {
  await login(page);
  const destinations = [
    ['Học sinh', '/students'],
    ['Học vụ', '/academics'],
    ['Lịch học', '/timetable'],
    ['Điểm số', '/grades'],
    ['Đơn từ', '/forms'],
    ['Thông báo', '/notifications'],
    ['Sự kiện & CLB', '/activities'],
    ['Nhật ký', '/audit'],
  ] as const;

  for (const [label, path] of destinations) {
    await page.getByRole('button', { name: label, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`${path}$`));
    await expect(page.getByRole('heading', { name: label, exact: true })).toBeVisible();
  }
});

test('recovers after a dashboard API failure', async ({ page }) => {
  let failuresRemaining = 2;
  await page.route('**/api/v1/admin/dashboard', async (route) => {
    if (failuresRemaining > 0) {
      failuresRemaining -= 1;
      await route.fulfill({ status: 503, contentType: 'application/problem+json', body: JSON.stringify({ code: 'TEMPORARY_FAILURE', detail: 'Unavailable' }) });
      return;
    }
    await route.continue();
  });

  await login(page);
  await expect(page.getByRole('alert')).toContainText('Không thể tải dữ liệu tổng quan');
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Dữ liệu thời gian thực')).toBeVisible();
});
