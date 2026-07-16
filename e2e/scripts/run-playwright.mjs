import path from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';

import { createIsolatedBackendEnvironment } from './backend-environment.mjs';
import { requireLoopbackHttpOrigin } from './origin.mjs';
import {
  backendRoot,
  e2eRoot,
  requireE2eComposeProjectName,
  requirePath,
} from './project-paths.mjs';
import { spawnLongRunningProcess } from './process-runner.mjs';
import { buildFlutterWeb, startFlutterWebServer } from './start-flutter-web.mjs';

function requirePositiveInteger(value, variableName, fallback) {
  const parsed = Number(value ?? fallback);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new Error(`${variableName} must be a positive integer.`);
  }
  return parsed;
}

function describeProcessFailure(label, result) {
  if (result.error) {
    return `${label} could not be started: ${result.error.message}`;
  }
  if (result.signal) {
    return `${label} stopped unexpectedly with signal ${result.signal}.`;
  }
  return `${label} stopped unexpectedly with exit code ${result.code ?? 1}.`;
}

async function waitForHttp(url, { label, process, timeoutMs, signal }) {
  const deadline = Date.now() + timeoutMs;

  while (Date.now() < deadline) {
    if (signal.aborted) throw signal.reason;
    if (process?.hasExited) {
      throw new Error(describeProcessFailure(label, process.result));
    }

    try {
      const response = await fetch(url, {
        signal: AbortSignal.timeout(Math.min(3_000, timeoutMs)),
      });
      if (response.ok) return;
    } catch {
      // Startup is still in progress. The deadline produces the actionable error.
    }

    await delay(250, undefined, { signal });
  }

  throw new Error(`${label} was not ready at ${url} within ${timeoutMs} ms.`);
}

async function stopQuietly(label, action) {
  try {
    await action();
  } catch (error) {
    console.error(`${label} cleanup failed:`, error);
  }
}

const composeProjectName = requireE2eComposeProjectName(
  process.env.E2E_COMPOSE_PROJECT_NAME ?? `myschool-e2e-${process.pid}`,
);
process.env.E2E_COMPOSE_PROJECT_NAME = composeProjectName;

const apiBaseUrl = requireLoopbackHttpOrigin(
  process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080',
  'E2E_API_BASE_URL',
);
const webBaseUrl = requireLoopbackHttpOrigin(
  process.env.E2E_WEB_BASE_URL ?? 'http://127.0.0.1:4173',
  'E2E_WEB_BASE_URL',
);
const reuseExistingServers = process.env.E2E_REUSE_SERVERS === 'true';
const backendStartupTimeoutMs = requirePositiveInteger(
  process.env.E2E_BACKEND_START_TIMEOUT_MS,
  'E2E_BACKEND_START_TIMEOUT_MS',
  180_000,
);
const flutterStartupTimeoutMs = requirePositiveInteger(
  process.env.E2E_FLUTTER_START_TIMEOUT_MS,
  'E2E_FLUTTER_START_TIMEOUT_MS',
  180_000,
);
const isWindows = process.platform === 'win32';
const mavenWrapper = path.join(backendRoot, isWindows ? 'mvnw.cmd' : 'mvnw');
const playwrightBinary = path.join(
  e2eRoot,
  'node_modules',
  '.bin',
  isWindows ? 'playwright.cmd' : 'playwright',
);

requirePath(path.join(backendRoot, 'pom.xml'), 'Backend pom.xml');
requirePath(mavenWrapper, 'Backend Maven wrapper');
requirePath(playwrightBinary, 'Playwright executable');

let postgresLifecycle;
let backend;
let flutterServer;
let playwright;
let shutdownSignal;
const shutdownController = new AbortController();

function requestShutdown(signal) {
  if (shutdownSignal) return;
  shutdownSignal = signal;
  shutdownController.abort(new Error(`E2E runner received ${signal}.`));
  if (playwright && !playwright.hasExited) {
    void playwright.stop();
  }
}

const signalHandlers = new Map(
  ['SIGINT', 'SIGTERM'].map((signal) => [
    signal,
    () => requestShutdown(signal),
  ]),
);
for (const [signal, handler] of signalHandlers) {
  process.once(signal, handler);
}

let exitCode = 0;
try {
  if (reuseExistingServers) {
    console.log('Reusing the supplied loopback E2E servers.');
  } else {
    postgresLifecycle = await import('./prepare-postgres.mjs');
    const postgres = await postgresLifecycle.preparePostgres();
    if (shutdownController.signal.aborted) throw shutdownController.signal.reason;

    const backendEnvironment = createIsolatedBackendEnvironment({
      apiBaseUrl,
      postgres,
      webBaseUrl,
    });
    console.log('Starting the Spring Boot backend with the e2e profile...');
    backend = spawnLongRunningProcess(
      mavenWrapper,
      [
        '--batch-mode',
        '--no-transfer-progress',
        '-Dspring-boot.run.profiles=e2e',
        'spring-boot:run',
      ],
      { cwd: backendRoot, env: backendEnvironment },
    );
    await waitForHttp(`${apiBaseUrl.origin}/actuator/health`, {
      label: 'Spring Boot backend',
      process: backend,
      signal: shutdownController.signal,
      timeoutMs: backendStartupTimeoutMs,
    });

    if (shutdownController.signal.aborted) throw shutdownController.signal.reason;
    const flutterBuild = buildFlutterWeb();
    if (shutdownController.signal.aborted) throw shutdownController.signal.reason;
    flutterServer = await startFlutterWebServer(flutterBuild);
    await waitForHttp(webBaseUrl.origin, {
      label: 'Flutter Web server',
      signal: shutdownController.signal,
      timeoutMs: flutterStartupTimeoutMs,
    });
  }

  if (shutdownController.signal.aborted) throw shutdownController.signal.reason;
  playwright = spawnLongRunningProcess(
    playwrightBinary,
    ['test', ...process.argv.slice(2)],
    {
      cwd: e2eRoot,
      env: { ...process.env, E2E_RUNNER_ACTIVE: 'true' },
    },
  );
  const testResult = await playwright.exited;
  if (testResult.error) {
    throw new Error(describeProcessFailure('Playwright', testResult));
  }
  if (shutdownSignal) {
    exitCode = 128;
  } else if (testResult.code !== 0) {
    exitCode = testResult.code ?? 1;
  }
} catch (error) {
  if (shutdownSignal) {
    exitCode = 128;
  } else {
    exitCode = 1;
    console.error('E2E lifecycle failed:', error);
  }
} finally {
  if (playwright && !playwright.hasExited) {
    await stopQuietly('Playwright', () => playwright.stop());
  }
  if (flutterServer) {
    await stopQuietly('Flutter Web server', () => flutterServer.close());
  }
  if (backend && !backend.hasExited) {
    await stopQuietly('Spring Boot backend', () => backend.stop());
  }
  if (!reuseExistingServers && postgresLifecycle) {
    await stopQuietly('PostgreSQL E2E resources', () =>
      postgresLifecycle.cleanupPostgres(),
    );
  }

  for (const [signal, handler] of signalHandlers) {
    process.removeListener(signal, handler);
  }
}

process.exitCode = exitCode;
