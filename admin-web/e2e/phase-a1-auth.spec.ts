import { expect, test } from '@playwright/test';

const adminPhone = '0900000000';
const adminPassword = 'Admin@123';
const studentPhone = '0912345678';
const studentPassword = '123';

test('redirects anonymous users to login and shows validation', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible();

  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();
  await expect(page.getByText('Vui lòng nhập số điện thoại hợp lệ.')).toBeVisible();
  await expect(page.getByText('Vui lòng nhập mật khẩu.')).toBeVisible();
});

test('shows a safe error when student credentials are used on admin login', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Số điện thoại').fill(studentPhone);
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill(studentPassword);
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();

  await expect(page.getByRole('alert')).toContainText('Số điện thoại hoặc mật khẩu không đúng.');
  await expect(page).toHaveURL(/\/login$/);
});

test('returns 401 to anonymous and 403 to student tokens on admin resources', async ({ request }) => {
  const anonymous = await request.get('/api/v1/admin/auth/me');
  expect(anonymous.status()).toBe(401);

  const studentLogin = await request.post('/api/v1/auth/login', {
    data: { phoneNumber: studentPhone, password: studentPassword },
  });
  expect(studentLogin.status()).toBe(200);
  const { accessToken } = await studentLogin.json() as { accessToken: string };

  const studentAccess = await request.get('/api/v1/admin/auth/me', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  expect(studentAccess.status()).toBe(403);
});

test('logs in, restores the cookie session after reload, and logs out', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Số điện thoại').fill(adminPhone);
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill(adminPassword);
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole('heading', { name: /Chào buổi/ })).toBeVisible();
  await expect(page.getByText('Đây là tình hình vận hành mới nhất của nhà trường.')).toBeVisible();

  const cookies = await page.context().cookies();
  const refreshCookie = cookies.find((cookie) => cookie.name === 'MYSCHOOL_ADMIN_REFRESH');
  expect(refreshCookie?.httpOnly).toBe(true);
  expect(refreshCookie?.sameSite).toBe('Strict');

  await page.reload();
  await expect(page.getByRole('heading', { name: /Chào buổi/ })).toBeVisible();

  await page.getByRole('button', { name: 'Mở menu tài khoản' }).click();
  await page.getByRole('menuitem', { name: 'Đăng xuất' }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);
});
