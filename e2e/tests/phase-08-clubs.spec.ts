import { expect, test, type Locator, type Page } from '@playwright/test';

import { AuthApi, expectTokenResponse, seededPeerStudent, seededStudent } from './support/auth-api.js';
import { ClubsApi, expectClub, expectClubs } from './support/clubs-api.js';
import { clubsPage, enableFlutterSemantics, homePage, loginPage } from './support/flutter-semantics.js';

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080';

async function enter(field: Locator, value: string): Promise<void> {
  await field.click();
  await field.press('ControlOrMeta+A');
  await field.pressSequentially(value);
}

async function loginUi(page: Page): Promise<void> {
  const login = loginPage(page);
  await enter(login.phone, seededStudent.phoneNumber);
  await enter(login.password, seededStudent.password);
  await login.loginButton.click();
  await expect(homePage(page).screen).toBeVisible();
}

test.describe('Phase 08 clubs and membership applications', () => {
  test('scopes clubs by JWT grade and exposes membership and capacity states', async ({ request }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const api = new ClubsApi(request, apiBaseUrl);
    const student = await expectTokenResponse(await auth.login(seededStudent.password));
    const peer = await expectTokenResponse(await auth.loginWithCredentials(seededPeerStudent));
    const clubs = await expectClubs(await api.list(student.accessToken));
    const peerClubs = await expectClubs(await api.list(peer.accessToken));
    expect(clubs.clubs.length).toBeGreaterThan(0);
    expect(await expectClubs(await api.listWithStudentId(student.accessToken, peer.student.id)))
      .toEqual(clubs);

    const peerOnly = peerClubs.clubs.find((club) => !clubs.clubs.some((item) => item.id === club.id));
    if (peerOnly === undefined) throw new Error('Expected a grade-11-only club.');
    const hiddenResponse = await api.get(student.accessToken, peerOnly.id);
    expect(hiddenResponse.status()).toBe(404);

    for (const category of [...new Set(clubs.clubs.map((club) => club.category))]) {
      const filtered = await expectClubs(await api.list(student.accessToken, category));
      expect(filtered.clubs.every((club) => club.category === category)).toBe(true);
    }
    expect(clubs.clubs.some((club) => club.membershipStatus === 'ACTIVE')).toBe(true);
    expect(clubs.clubs.some((club) => club.membershipStatus === 'PENDING')).toBe(true);
    expect(clubs.clubs.some((club) => club.capacity === club.activeMemberCount && !club.canApply)).toBe(true);
  });

  test('applies, withdraws, and reapplies through the API', async ({ request }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const api = new ClubsApi(request, apiBaseUrl);
    const student = await expectTokenResponse(await auth.login(seededStudent.password));
    const clubs = await expectClubs(await api.list(student.accessToken));
    const open = clubs.clubs.find((club) => club.canApply);
    if (open === undefined) throw new Error('Expected an open club.');
    const pending = await expectClub(await api.apply(student.accessToken, open.id), 201);
    expect(pending.membershipStatus).toBe('PENDING');
    const withdrawn = await expectClub(await api.withdraw(student.accessToken, open.id));
    expect(withdrawn.membershipStatus).toBe('WITHDRAWN');
    const reapplied = await expectClub(await api.apply(student.accessToken, open.id));
    expect(reapplied.membershipStatus).toBe('PENDING');
    const cleanup = await expectClub(await api.withdraw(student.accessToken, open.id));
    expect(cleanup.membershipStatus).toBe('WITHDRAWN');
  });

  test('opens, applies, withdraws, and reloads a club through Flutter', async ({ page, request }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const api = new ClubsApi(request, apiBaseUrl);
    const student = await expectTokenResponse(await auth.login(seededStudent.password));
    const available = await expectClubs(await api.list(student.accessToken));
    const club = available.clubs.find((item) => item.canApply);
    if (club === undefined) throw new Error('Expected an open club for Flutter.');

    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginUi(page);
    await homePage(page).quickAction('Hoạt động').click();
    await page.getByRole('button', { name: 'Câu lạc bộ', exact: true }).click();
    const clubs = clubsPage(page);
    await expect(clubs.screen).toBeVisible();
    await clubs.detailsButton(club).click();
    await expect(clubs.detailScreen(club)).toBeVisible();
    await clubs.applyButton.click();
    await expect(clubs.statusText('Chờ duyệt')).toBeVisible();
    await clubs.withdrawButton.click();
    await expect(clubs.statusText('Đã rút đơn')).toBeVisible();

    await page.reload();
    await enableFlutterSemantics(page, clubsPage(page).detailScreen(club));
    await expect(clubsPage(page).statusText('Đã rút đơn')).toBeVisible();
  });
});
