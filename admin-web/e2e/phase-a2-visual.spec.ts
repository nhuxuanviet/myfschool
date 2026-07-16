import { expect, test } from '@playwright/test';

test.use({ viewport: { width: 1920, height: 900 } });

test('captures the refined login and dashboard at the review viewport', async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on('console', (message) => { if (message.type() === 'error') consoleErrors.push(message.text()); });

  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible();
  await page.screenshot({ path: '../design-qa/admin-a2-login.png', fullPage: true });

  await page.getByLabel('Số điện thoại').fill('0900000000');
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill('Admin@123');
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();
  await expect(page.getByText('Dữ liệu thời gian thực')).toBeVisible();
  await page.screenshot({ path: '../design-qa/admin-a2-dashboard.png', fullPage: true });

  const hasHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  expect(hasHorizontalOverflow).toBe(false);
  expect(consoleErrors).toEqual([]);
});

test('keeps the admin shell usable at 1024 by 768', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/login');
  await page.getByLabel('Số điện thoại').fill('0900000000');
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill('Admin@123');
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();
  await expect(page.getByRole('button', { name: 'Thu gọn menu' })).toBeVisible();
  await expect(page.getByText('Dữ liệu thời gian thực')).toBeVisible();
  const hasHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  expect(hasHorizontalOverflow).toBe(false);
});
