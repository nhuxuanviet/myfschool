import { expect, test, type Locator, type Page } from '@playwright/test';

import {
  AuthApi,
  expectTokenResponse,
  seededPeerStudent,
  seededStudent,
} from './support/auth-api.js';
import {
  eventCategories,
  EventsApi,
  expectEventNotFound,
  expectEventRegistrationConflict,
  expectEventResponse,
  expectEventsResponse,
  expectEventsUnauthorized,
  expectSameEvent,
  findCancellationDeadlineBlockedEvent,
  findCapacityBlockedEvent,
  findFreshOpenCancellableEvent,
  findOpenCancellableEvent,
  findPastEvent,
  findRegistrationDeadlineBlockedEvent,
  type SchoolEvent,
} from './support/events-api.js';
import {
  enableFlutterSemantics,
  eventsPage,
  homePage,
  loginPage,
} from './support/flutter-semantics.js';

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080';
const studentIdProbe = '00000000-0000-0000-0000-000000000999';

async function enterFlutterText(field: Locator, value: string): Promise<void> {
  await field.click();
  await field.press('ControlOrMeta+A');
  await field.pressSequentially(value);
  await expect(field).toHaveValue(value);
}

async function loginThroughUi(page: Page): Promise<void> {
  const login = loginPage(page);
  await enterFlutterText(login.phone, seededStudent.phoneNumber);
  await enterFlutterText(login.password, seededStudent.password);
  await login.loginButton.click();
  await expect(homePage(page).screen).toBeVisible();
  await expect(page).toHaveURL(/\/home$/);
}

async function expectEventDetail(
  page: Page,
  event: SchoolEvent,
): Promise<void> {
  await expect(eventsPage(page).detailScreen(event)).toBeVisible();
}

test.describe('Phase 06 events and announcements', () => {
  test('filters student-visible events and exposes capacity and deadline actions', async ({
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const eventsApi = new EventsApi(request, apiBaseUrl);

    await expectEventsUnauthorized(await eventsApi.listUnauthenticated());

    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const peerTokenResponse = await expectTokenResponse(
      await auth.loginWithCredentials(seededPeerStudent),
    );
    expect(peerTokenResponse.student.gradeLevel).toBe(
      seededPeerStudent.gradeLevel,
    );
    const allEvents = await expectEventsResponse(
      await eventsApi.list(tokenResponse.accessToken, { includePast: true }),
    );
    const peerEvents = await expectEventsResponse(
      await eventsApi.list(peerTokenResponse.accessToken, {
        includePast: true,
      }),
    );
    const currentEvents = await expectEventsResponse(
      await eventsApi.list(tokenResponse.accessToken),
    );
    expect(allEvents.events).not.toHaveLength(0);

    const allEventIds = new Set(allEvents.events.map((event) => event.id));
    for (const event of currentEvents.events) {
      expect(allEventIds.has(event.id)).toBe(true);
    }
    const pastEvent = findPastEvent(allEvents.events);
    expect(currentEvents.events.map((event) => event.id)).not.toContain(
      pastEvent.id,
    );

    for (const event of allEvents.events) {
      expect(
        event.audienceGradeLevel === null ||
          event.audienceGradeLevel === tokenResponse.student.gradeLevel,
        `${event.title} must be visible only to its intended grade or all grades`,
      ).toBe(true);
    }
    const peerOnlyEvent = peerEvents.events.find(
      (event) =>
        event.category === 'CLUB' &&
        event.audienceGradeLevel === peerTokenResponse.student.gradeLevel,
    );
    if (peerOnlyEvent === undefined) {
      throw new Error(
        'Expected a grade-11-only event visible to the peer student.',
      );
    }
    await expectEventNotFound(
      await eventsApi.get(tokenResponse.accessToken, peerOnlyEvent.id),
      peerOnlyEvent.id,
    );

    for (const category of eventCategories(allEvents.events)) {
      const filtered = await expectEventsResponse(
        await eventsApi.list(tokenResponse.accessToken, {
          category,
          includePast: true,
        }),
      );
      for (const event of filtered.events) {
        expect(event.category).toBe(category);
        expect(allEventIds.has(event.id)).toBe(true);
      }
    }

    const visibleEvent = allEvents.events[0];
    const detail = await expectEventResponse(
      await eventsApi.get(tokenResponse.accessToken, visibleEvent.id),
    );
    expectSameEvent(detail, visibleEvent);

    const injectedList = await expectEventsResponse(
      await eventsApi.listWithStudentId(tokenResponse.accessToken, studentIdProbe, {
        includePast: true,
      }),
    );
    expect(injectedList).toEqual(allEvents);
    const injectedDetail = await expectEventResponse(
      await eventsApi.getWithStudentId(
        tokenResponse.accessToken,
        visibleEvent.id,
        studentIdProbe,
      ),
    );
    expectSameEvent(injectedDetail, visibleEvent);

    const capacityBlocked = findCapacityBlockedEvent(peerEvents.events);
    expect(capacityBlocked.canRegister).toBe(false);
    await expectEventRegistrationConflict(
      await eventsApi.register(
        peerTokenResponse.accessToken,
        capacityBlocked.id,
      ),
      capacityBlocked.id,
      'EVENT_CAPACITY_REACHED',
    );
    const registrationDeadlineBlocked = findRegistrationDeadlineBlockedEvent(
      allEvents.events,
    );
    expect(registrationDeadlineBlocked.canRegister).toBe(false);
    await expectEventRegistrationConflict(
      await eventsApi.register(
        tokenResponse.accessToken,
        registrationDeadlineBlocked.id,
      ),
      registrationDeadlineBlocked.id,
      'EVENT_REGISTRATION_CLOSED',
    );
    const cancellationDeadlineBlocked = findCancellationDeadlineBlockedEvent(
      allEvents.events,
    );
    expect(cancellationDeadlineBlocked.canCancel).toBe(false);
    await expectEventRegistrationConflict(
      await eventsApi.cancel(
        tokenResponse.accessToken,
        cancellationDeadlineBlocked.id,
      ),
      cancellationDeadlineBlocked.id,
      'EVENT_CANCELLATION_CLOSED',
    );
  });

  test('registers and cancels a newly open event through the API', async ({
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const eventsApi = new EventsApi(request, apiBaseUrl);
    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const events = await expectEventsResponse(
      await eventsApi.list(tokenResponse.accessToken),
    );
    const event = findFreshOpenCancellableEvent(events.events);

    const registration = await expectEventResponse(
      await eventsApi.register(tokenResponse.accessToken, event.id),
      201,
    );
    expect(registration.id).toBe(event.id);
    expect(registration.registrationStatus).toBe('REGISTERED');
    expect(registration.registeredCount).toBe(event.registeredCount + 1);
    expect(registration.canRegister).toBe(false);
    expect(registration.canCancel).toBe(true);

    const cancellation = await expectEventResponse(
      await eventsApi.cancel(tokenResponse.accessToken, event.id),
    );
    expect(cancellation.id).toBe(event.id);
    expect(cancellation.registrationStatus).toBe('CANCELLED');
    expect(cancellation.registeredCount).toBe(event.registeredCount);
    expect(cancellation.canCancel).toBe(false);
  });

  test('filters, registers, cancels, and reloads an event detail through Flutter', async ({
    page,
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const eventsApi = new EventsApi(request, apiBaseUrl);
    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const apiEvents = await expectEventsResponse(
      await eventsApi.list(tokenResponse.accessToken),
    );
    const event = findOpenCancellableEvent(apiEvents.events);

    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginThroughUi(page);

    const events = eventsPage(page);
    await homePage(page).quickAction('Hoạt động').click();
    await expect(page).toHaveURL(/\/events$/);
    await expect(events.screen).toBeVisible();
    await events.filterButton.click();
    await expect(events.filterOption(event.category)).toBeVisible();
    await events.filterOption(event.category).click();
    await page.getByRole('button', { name: 'Xem kết quả', exact: true }).click();
    await expect(events.eventDetailsButton(event)).toBeVisible();

    await events.eventDetailsButton(event).click();
    const expectedDetailHash = `#/events/${encodeURIComponent(event.id)}`;
    await expect(page).toHaveURL((url) => url.hash === expectedDetailHash);
    await expectEventDetail(page, event);

    await expect(events.registerButton).toBeVisible();
    await events.registerButton.click();
    await expect(events.cancelButton).toBeVisible();

    await events.cancelButton.click();
    await expect(events.registerButton).toBeVisible();
    const cancellation = await expectEventResponse(
      await eventsApi.get(tokenResponse.accessToken, event.id),
    );
    expect(cancellation.registrationStatus).toBe('CANCELLED');

    await page.reload();
    await enableFlutterSemantics(page, eventsPage(page).detailScreen(event));
    await expect(page).toHaveURL((url) => url.hash === expectedDetailHash);
    await expectEventDetail(page, event);
    await expect(eventsPage(page).registerButton).toBeVisible();
  });
});
