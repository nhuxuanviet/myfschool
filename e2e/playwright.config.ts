import { defineConfig, devices } from '@playwright/test';

process.env.E2E_COMPOSE_PROJECT_NAME ??= `myschool-e2e-${process.pid}`;
if (
  !/^myschool-e2e(?:-[a-z0-9][a-z0-9_-]*)?$/.test(
    process.env.E2E_COMPOSE_PROJECT_NAME,
  )
) {
  throw new Error(
    'E2E_COMPOSE_PROJECT_NAME must use the reserved myschool-e2e-* prefix.',
  );
}

function requireLoopbackOrigin(value: string, label: string): string {
  const url = new URL(value);
  const loopbackHosts = new Set(['127.0.0.1', 'localhost', '[::1]']);
  const port = Number(url.port);
  if (
    url.protocol !== 'http:' ||
    !loopbackHosts.has(url.hostname) ||
    !Number.isInteger(port) ||
    port < 1 ||
    port > 65_535 ||
    url.username ||
    url.password ||
    (url.pathname !== '/' && url.pathname !== '') ||
    url.search ||
    url.hash
  ) {
    throw new Error(
      `${label} must be an HTTP loopback origin with an explicit valid port.`,
    );
  }
  return url.origin;
}

const webBaseUrl = requireLoopbackOrigin(
  process.env.E2E_WEB_BASE_URL ?? 'http://127.0.0.1:4173',
  'E2E_WEB_BASE_URL',
);

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI
    ? [['line'], ['html', { open: 'never' }]]
    : [['list'], ['html', { open: 'never' }]],
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL: webBaseUrl,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 390, height: 844 },
      },
    },
  ],
});
