import { expect, test, type Page } from '@playwright/test';

async function loginAdmin(page: Page): Promise<void> {
  await page.context().clearCookies();
  await page.goto('/login');
  await page.getByLabel('Số điện thoại').fill('0900000000');
  await page.getByRole('textbox', { name: 'Mật khẩu', exact: true }).fill('Admin@123');
  await page.getByRole('button', { name: 'Đăng nhập an toàn' }).click();
  await expect(page).toHaveURL(new RegExp('/$'));
}

test('Admin can inspect and update safe AI runtime settings without receiving a secret', async ({ page }) => {
  await loginAdmin(page);

  const settingsResponses: string[] = [];
  page.on('response', async (response) => {
    if (response.url().includes('/api/v1/admin/ai/settings')) {
      settingsResponses.push(await response.text());
    }
  });

  await page.goto('/ai-settings');
  await expect(page.getByRole('heading', { name: 'Cấu hình AI', exact: true })).toBeVisible();
  await expect(page.getByText('Khóa API không bao giờ được gửi tới trình duyệt')).toBeVisible();
  await expect(page.getByLabel('Model')).toHaveValue(/.+/);

  const memoryInput = page.getByLabel('Số tin nhắn ghi nhớ');
  const originalMemory = await memoryInput.inputValue();
  const temporaryMemory = originalMemory === '14' ? '12' : '14';
  await memoryInput.fill(temporaryMemory);
  await page.getByRole('button', { name: 'Lưu cấu hình' }).click();
  await expect(page.getByText('Đã cập nhật cấu hình AI.')).toBeVisible();
  await expect(memoryInput).toHaveValue(temporaryMemory);

  await memoryInput.fill(originalMemory);
  await page.getByRole('button', { name: 'Lưu cấu hình' }).click();
  await expect(page.getByText('Đã cập nhật cấu hình AI.')).toBeVisible();

  expect(settingsResponses.length).toBeGreaterThan(1);
  for (const body of settingsResponses) {
    expect(body).not.toMatch(/"apiKey"\s*:/);
    expect(body).not.toContain('sk-');
  }
});
