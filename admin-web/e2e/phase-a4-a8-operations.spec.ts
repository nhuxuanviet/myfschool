import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const apiBaseUrl = 'http://127.0.0.1:8080';

async function loginAdmin(page: Page): Promise<void> {
  await page.context().clearCookies();
  await page.goto('/login');
  await page.getByLabel('Số điện thoại').fill('0900000000');
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill('Admin@123');
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();
  await expect(page).toHaveURL(new RegExp('/$'));
}

async function studentToken(request: APIRequestContext): Promise<string> {
  for (const password of ['123', 'Student@123']) {
    const response = await request.post(`${apiBaseUrl}/api/v1/auth/login`, {
      data: { phoneNumber: '0912345678', password },
    });
    if (response.ok()) return (await response.json()).accessToken as string;
  }
  throw new Error('Seeded student could not log in');
}

test('all remaining Admin modules render real data without browser errors', async ({ page }) => {
  const errors: string[] = [];
  page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()); });
  await loginAdmin(page);
  for (const [path, heading] of [
    ['/timetable', 'Lịch học'], ['/grades', 'Điểm số'], ['/forms', 'Đơn từ'],
    ['/notifications', 'Thông báo'], ['/activities', 'Sự kiện & CLB'], ['/ai-settings', 'Cấu hình AI'], ['/audit', 'Nhật ký'],
  ] as const) {
    await page.goto(path);
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
    await expect(page.locator('main')).not.toContainText('sẽ được triển khai');
  }
  expect(errors).toEqual([]);
});

test('Admin mutations flow through the same data consumed by students', async ({ page, request }) => {
  test.setTimeout(90_000);
  await loginAdmin(page);
  const suffix = Date.now().toString().slice(-6);

  await page.goto('/timetable');
  await expect(page.getByRole('heading', { name: 'Lịch học', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Thêm tiết học' }).click();
  const lessonDialog = page.getByRole('dialog');
  await lessonDialog.getByLabel('Ngày').click();
  await page.getByRole('option', { name: 'Thứ bảy' }).click();
  await lessonDialog.getByLabel('Buổi').click();
  await page.getByRole('option', { name: 'Chiều' }).click();
  await lessonDialog.getByLabel('Tiết').click();
  await page.getByRole('option', { name: '5', exact: true }).click();
  await lessonDialog.getByLabel('Môn học').click();
  await page.getByRole('option').first().click();
  await lessonDialog.getByLabel('Giáo viên').fill(`GV E2E ${suffix}`);
  await lessonDialog.getByLabel('Phòng học').fill('P.E2E');
  await lessonDialog.getByRole('button', { name: 'Lưu tiết học' }).click();
  await expect(page.getByText('Đã thêm tiết học.')).toBeVisible();
  await expect(page.getByText(`GV E2E ${suffix}`, { exact: false })).toBeVisible();

  const token = await studentToken(request);
  const timetable = await request.get(`${apiBaseUrl}/api/v1/timetable?weekStart=2026-07-13`, { headers: { Authorization: `Bearer ${token}` } });
  expect(timetable.ok()).toBe(true);
  expect(JSON.stringify(await timetable.json())).toContain('P.E2E');
  const lessonCard = page.getByText(`GV E2E ${suffix}`, { exact: false }).locator('..');
  await lessonCard.getByRole('button', { name: /Xóa tiết/ }).click();

  await page.goto('/grades');
  await expect(page.getByRole('heading', { name: 'Điểm số', exact: true })).toBeVisible();
  await page.getByRole('combobox', { name: 'Học sinh', exact: true }).click();
  await page.getByRole('option', { name: /Nhữ Xuân Việt/ }).click();
  await expect(page.getByRole('button', { name: 'Nhập điểm' })).toBeEnabled();
  await page.getByRole('button', { name: 'Nhập điểm' }).click();
  const gradeDialog = page.getByRole('dialog');
  const gradeLabel = `Đánh giá E2E ${suffix}`;
  await gradeDialog.getByLabel('Tên cột điểm').fill(gradeLabel);
  await gradeDialog.getByRole('spinbutton', { name: 'Điểm', exact: true }).fill('9.2');
  await gradeDialog.getByRole('button', { name: 'Ghi nhận điểm' }).click();
  await expect(page.getByText('Đã ghi nhận điểm thành phần.')).toBeVisible();
  await expect(page.getByText(gradeLabel, { exact: true })).toBeVisible();
  const grades = await request.get(`${apiBaseUrl}/api/v1/grades`, { headers: { Authorization: `Bearer ${token}` } });
  expect(grades.ok()).toBe(true);
  expect(JSON.stringify(await grades.json())).toContain(gradeLabel);

  await page.goto('/notifications');
  const announcementTitle = `Thông báo E2E ${suffix}`;
  await page.getByRole('button', { name: 'Soạn thông báo' }).click();
  const announcementDialog = page.getByRole('dialog');
  await announcementDialog.getByLabel('Tiêu đề').fill(announcementTitle);
  await announcementDialog.getByLabel('Nội dung').fill('Nội dung được kiểm tra xuyên Admin và ứng dụng học sinh.');
  await announcementDialog.getByRole('button', { name: 'Xuất bản' }).click();
  await expect(page.getByText('Thông báo đã được xuất bản cho ứng dụng học sinh.')).toBeVisible();
  const home = await request.get(`${apiBaseUrl}/api/v1/home`, { headers: { Authorization: `Bearer ${token}` } });
  expect(home.ok()).toBe(true);
  expect(JSON.stringify(await home.json())).toContain(announcementTitle);
  await page.getByRole('button', { name: `Xóa thông báo ${announcementTitle}` }).click();

  await page.goto('/activities');
  const eventTitle = `Sự kiện E2E ${suffix}`;
  await page.getByRole('button', { name: 'Thêm sự kiện' }).click();
  const eventDialog = page.getByRole('dialog');
  await eventDialog.getByLabel('Tên sự kiện').fill(eventTitle);
  await eventDialog.getByLabel('Mô tả').fill('Sự kiện kiểm thử Playwright');
  await eventDialog.getByLabel('Địa điểm').fill('Hội trường E2E');
  await eventDialog.getByRole('button', { name: 'Tạo sự kiện' }).click();
  await expect(page.getByText(eventTitle, { exact: true })).toBeVisible();
  const events = await request.get(`${apiBaseUrl}/api/v1/events`, { headers: { Authorization: `Bearer ${token}` } });
  expect(events.ok()).toBe(true);
  expect(JSON.stringify(await events.json())).toContain(eventTitle);

  const clubName = `CLB E2E ${suffix}`;
  await page.getByRole('button', { name: 'Thêm CLB' }).click();
  const clubDialog = page.getByRole('dialog');
  await clubDialog.getByLabel('Tên CLB').fill(clubName);
  await clubDialog.getByLabel('Mô tả').fill('Câu lạc bộ kiểm thử Playwright');
  await clubDialog.getByLabel('Giáo viên phụ trách').fill('Thầy E2E');
  await clubDialog.getByLabel('Lịch sinh hoạt').fill('Thứ bảy 15:00');
  await clubDialog.getByLabel('Địa điểm').fill('P.CLBE2E');
  await clubDialog.getByRole('button', { name: 'Tạo CLB' }).click();
  await page.getByRole('tab', { name: /Câu lạc bộ/ }).click();
  await expect(page.getByText(clubName, { exact: true })).toBeVisible();
  const clubs = await request.get(`${apiBaseUrl}/api/v1/clubs`, { headers: { Authorization: `Bearer ${token}` } });
  expect(clubs.ok()).toBe(true);
  expect(JSON.stringify(await clubs.json())).toContain(clubName);

  await page.goto('/forms');
  await expect(page.getByText('Danh sách đơn')).toBeVisible();
  await page.getByLabel('Trạng thái').click();
  await page.getByRole('option', { name: 'Đang xử lý' }).click();

  await page.goto('/audit');
  await expect(page.getByText('TIMETABLE_LESSON', { exact: true }).first()).toBeVisible();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Xuất CSV' }).click();
  expect((await download).suggestedFilename()).toBe('nhat-ky-quan-tri.csv');
});

test('remaining Admin pages stay usable at tablet width', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await loginAdmin(page);
  for (const [path, heading] of [
    ['/timetable', 'Lịch học'], ['/grades', 'Điểm số'], ['/forms', 'Đơn từ'],
    ['/notifications', 'Thông báo'], ['/activities', 'Sự kiện & CLB'],
    ['/ai-settings', 'Cấu hình AI'], ['/audit', 'Nhật ký'],
  ] as const) {
    await page.goto(path);
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  }
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/timetable');
  await expect(page.getByRole('heading', { name: 'Lịch học', exact: true })).toBeVisible();
  await page.screenshot({ path: '../design-qa/admin-a4-timetable.png', fullPage: true });
  await page.goto('/activities');
  await expect(page.getByRole('heading', { name: 'Sự kiện & CLB', exact: true })).toBeVisible();
  await page.screenshot({ path: '../design-qa/admin-a7-activities.png', fullPage: true });
});
