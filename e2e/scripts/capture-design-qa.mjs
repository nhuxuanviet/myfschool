import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { chromium } from '@playwright/test';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, '..', '..');
const outputDirectory = path.join(projectRoot, 'design-qa');
const baseUrl = process.env.E2E_WEB_BASE_URL ?? 'http://127.0.0.1:4173';

fs.mkdirSync(outputDirectory, { recursive: true });

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
const browserErrors = [];

page.on('console', (message) => {
  if (message.type() === 'error') browserErrors.push(message.text());
});
page.on('pageerror', (error) => browserErrors.push(error.message));

async function enableSemantics() {
  await page.waitForLoadState('domcontentloaded');
  const accessibilityButton = page.getByRole('button', {
    name: 'Enable accessibility',
    exact: true,
  });
  const placeholder = page.locator('flt-semantics-placeholder');
  const semanticsNode = page.locator('flt-semantics[aria-label]');
  await accessibilityButton
    .or(placeholder)
    .or(semanticsNode)
    .first()
    .waitFor({ timeout: 30_000 });
  if (await accessibilityButton.isVisible().catch(() => false)) {
    await accessibilityButton.evaluate((element) => element.click());
  } else if (await placeholder.first().isVisible().catch(() => false)) {
    await placeholder.first().evaluate((element) => element.click());
  }
}

async function openRoute(route, readyLabel) {
  await page.goto(`${baseUrl}/#${route}`);
  await enableSemantics();
  await page.getByLabel(readyLabel, { exact: true }).first().waitFor();
  await page.waitForTimeout(350);
}

async function capture(name) {
  await page.screenshot({
    path: path.join(outputDirectory, `implementation-${name}.png`),
    fullPage: false,
  });
}

async function enterText(field, value) {
  await field.click();
  await field.press('Control+A');
  await field.pressSequentially(value);
}

try {
  await page.goto(`${baseUrl}/login`);
  await enableSemantics();
  await enterText(
    page.getByRole('textbox', { name: 'Số điện thoại', exact: true }),
    '0912345678',
  );
  await enterText(
    page.getByLabel('Mật khẩu', { exact: true }),
    '123',
  );
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await page.getByLabel('Trang chủ', { exact: true }).first().waitFor();
  await page.waitForTimeout(500);
  await capture('01-home');
  await page.getByRole('button', { name: 'Mở trợ lý AI', exact: true }).click();
  await page.getByLabel('Trợ lý học sinh', { exact: true }).waitFor();
  await capture('feedback-assistant-popup');
  await page.getByRole('button', { name: 'Đóng trợ lý', exact: true }).click();
  await page
    .getByRole('button', { name: 'Mở lịch học cho tiết học tiếp theo', exact: true })
    .hover();
  await capture('feedback-home-hover');
  await page.setViewportSize({ width: 500, height: 844 });
  await capture('feedback-home-wide');
  await page.setViewportSize({ width: 390, height: 844 });

  await openRoute('/notifications', 'Thông báo học sinh');
  await capture('feedback-notifications');

  await openRoute('/more', 'Trang cá nhân');
  await capture('feedback-profile');

  await openRoute('/schedule', 'Lịch học');
  await capture('02-schedule');
  await page.getByRole('button', { name: 'Chọn ngày', exact: true }).click();
  await page.getByRole('dialog').waitFor();
  await capture('feedback-date-picker');
  await page.getByRole('button', { name: 'Hủy', exact: true }).click();

  await openRoute('/grades', 'Điểm học kỳ');
  await capture('03-grades');
  await page.getByRole('button', { name: /^Xem chi tiết / }).first().click();
  await page.getByLabel(/^Chi tiết điểm /).waitFor();
  await capture('feedback-grade-detail');

  await openRoute('/events', 'Sự kiện');
  await capture('04-events-list');
  await page.getByRole('button', { name: 'Lọc sự kiện', exact: true }).click();
  await page.getByText('Bộ lọc sự kiện', { exact: true }).waitFor();
  await page.waitForTimeout(300);
  await capture('feedback-event-filter');
  await page.getByLabel('Hiển thị sự kiện đã qua', { exact: true }).click();
  await page.waitForTimeout(300);
  await capture('feedback-event-filter-enabled');
  await page.getByRole('button', { name: 'Xem kết quả', exact: true }).click();
  await page.getByRole('button', { name: /^Xem sự kiện / }).first().click();
  await page.getByLabel(/^Chi tiết sự kiện /).waitFor();
  await capture('05-event-detail');

  await openRoute('/clubs', 'Câu lạc bộ');
  await capture('06-clubs-list');
  await page.getByRole('button', { name: /^Xem CLB / }).first().click();
  await page.getByLabel(/^Chi tiết CLB /).waitFor();
  await page.waitForTimeout(700);
  await capture('07-club-detail');

  await openRoute('/forms', 'Đơn từ');
  await capture('08-forms-list');
  await page.getByRole('button', { name: 'Tạo đơn mới', exact: true }).click();
  await page.getByLabel('Tạo đơn học sinh', { exact: true }).waitFor();
  await capture('09-form-create');

  await openRoute('/forms', 'Đơn từ');
  await page.getByRole('button', { name: /^Xem đơn / }).first().click();
  await page.getByLabel(/^Chi tiết đơn /).waitFor();
  await capture('10-form-detail');

  await openRoute('/more', 'Trang cá nhân');
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await page.getByLabel('Màn hình đăng nhập', { exact: true }).waitFor();
  await page
    .getByRole('button', { name: 'Quên mật khẩu?', exact: true })
    .click();
  await page.getByRole('heading', { name: 'Đặt lại mật khẩu', exact: true }).waitFor();
  await capture('feedback-reset-phone');
  await enterText(
    page.getByRole('textbox', { name: 'Số điện thoại', exact: true }),
    '0912345678',
  );
  await page.getByRole('button', { name: 'Gửi mã OTP', exact: true }).click();
  await page.getByRole('textbox', { name: /^Mã OTP 1$/ }).waitFor();
  await page.waitForTimeout(350);
  await capture('feedback-reset-otp');
} finally {
  await browser.close();
}

if (browserErrors.length > 0) {
  throw new Error(`Browser errors detected:\n${browserErrors.join('\n')}`);
}
