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

test('creates a class and manages a student through the real API', async ({ page }) => {
  const consoleErrors: string[] = [];
  page.on('console', (message) => { if (message.type() === 'error') consoleErrors.push(message.text()); });
  await login(page);

  const suffix = Date.now().toString().slice(-7);
  const classCode = `10Z${suffix.slice(-3)}`;
  const studentCode = `A3${suffix}`;
  const studentPhone = `098${suffix}`;
  const initialName = 'Nguyễn Học Sinh A3';
  const updatedName = 'Nguyễn Học Sinh A3 Đã Sửa';

  await page.getByRole('button', { name: 'Học vụ', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Học vụ', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Lớp học', exact: true }).click();
  const classDialog = page.getByRole('dialog');
  await classDialog.getByLabel('Mã lớp').fill(classCode);
  await classDialog.getByLabel('Tên lớp').fill(`Lớp ${classCode}`);
  await classDialog.getByLabel('Khối').click();
  await page.getByRole('option', { name: 'Khối 10' }).click();
  await classDialog.getByRole('button', { name: 'Lưu dữ liệu' }).click();
  await expect(page.getByText(classCode, { exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Môn học', exact: true }).click();
  const subjectDialog = page.getByRole('dialog');
  await subjectDialog.getByLabel('Mã môn học').fill(`MH${suffix.slice(-5)}`);
  const subjectName = `Kỹ năng số ${suffix.slice(-3)}`;
  const updatedSubjectName = `${subjectName} cập nhật`;
  await subjectDialog.getByLabel('Tên môn học').fill(subjectName);
  await subjectDialog.getByRole('button', { name: 'Lưu dữ liệu' }).click();
  await expect(page.getByText('Đã tạo môn học.')).toBeVisible();
  await page.getByRole('tab', { name: 'Môn học' }).click();
  await page.getByRole('button', { name: `Sửa môn ${subjectName}` }).click();
  await page.getByRole('dialog').getByLabel('Tên môn học').fill(updatedSubjectName);
  await page.getByRole('dialog').getByRole('button', { name: 'Lưu dữ liệu' }).click();
  await expect(page.getByText(updatedSubjectName, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: `Xóa môn ${updatedSubjectName}` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Xóa dữ liệu' }).click();
  await expect(page.getByText(updatedSubjectName, { exact: true })).toHaveCount(0);

  await page.getByRole('button', { name: 'Học sinh', exact: true }).click();
  await page.getByRole('button', { name: 'Thêm học sinh' }).click();
  await page.getByLabel('Họ và tên').fill(initialName);
  await page.getByLabel('Mã học sinh').fill(studentCode);
  await page.getByLabel('Số điện thoại').fill(studentPhone);
  await page.getByLabel('Lớp học').click();
  await page.getByRole('option', { name: new RegExp(classCode) }).click();
  await page.getByRole('button', { name: 'Tạo học sinh' }).click();
  await expect(page.getByText('Đã tạo tài khoản học sinh.')).toBeVisible();

  await page.getByLabel('Tìm trong danh sách học sinh').fill(studentCode);
  await expect(page.getByRole('cell', { name: studentCode, exact: true })).toBeVisible();
  await page.getByRole('button', { name: `Sửa ${initialName}` }).click();
  await page.getByLabel('Họ và tên').fill(updatedName);
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Đã cập nhật hồ sơ học sinh.')).toBeVisible();
  await expect(page.getByText(updatedName, { exact: true })).toBeVisible();

  await page.reload();
  await expect(page).toHaveURL(new RegExp(`search=${studentCode}`));
  await expect(page.getByText(updatedName, { exact: true })).toBeVisible();
  expect(consoleErrors).toEqual([]);
});

test('keeps the A3 management screens usable at desktop and tablet widths', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await login(page);
  await page.goto('/students');
  await expect(page.getByRole('heading', { name: 'Học sinh', exact: true })).toBeVisible();
  await page.screenshot({ path: '../design-qa/admin-a3-students.png', fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);

  await page.goto('/academics');
  await expect(page.getByRole('heading', { name: 'Học vụ', exact: true })).toBeVisible();
  await page.screenshot({ path: '../design-qa/admin-a3-academics.png', fullPage: true });

  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/academics');
  await expect(page.getByRole('heading', { name: 'Học vụ', exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
});
