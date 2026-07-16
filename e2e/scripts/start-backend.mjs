import path from 'node:path';

import { createIsolatedBackendEnvironment } from './backend-environment.mjs';
import { requireLoopbackHttpOrigin } from './origin.mjs';
import { backendRoot, requirePath } from './project-paths.mjs';
import { cleanupPostgres, preparePostgres } from './prepare-postgres.mjs';
import { startLongRunningProcess } from './process-runner.mjs';

requirePath(path.join(backendRoot, 'pom.xml'), 'Backend pom.xml');

const apiBaseUrl = requireLoopbackHttpOrigin(
  process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080',
  'E2E_API_BASE_URL',
);
const webBaseUrl = requireLoopbackHttpOrigin(
  process.env.E2E_WEB_BASE_URL ?? 'http://127.0.0.1:4173',
  'E2E_WEB_BASE_URL',
);
const isWindows = process.platform === 'win32';
const mavenWrapper = path.join(backendRoot, isWindows ? 'mvnw.cmd' : 'mvnw');
requirePath(mavenWrapper, 'Backend Maven wrapper');

let backendStarted = false;
try {
  const postgres = await preparePostgres();
  const environment = createIsolatedBackendEnvironment({
    apiBaseUrl,
    postgres,
    webBaseUrl,
  });

  console.log('Starting the Spring Boot backend with the e2e profile...');
  startLongRunningProcess(
    mavenWrapper,
    [
      '--batch-mode',
      '--no-transfer-progress',
      '-Dspring-boot.run.profiles=e2e',
      'spring-boot:run',
    ],
    { cwd: backendRoot, env: environment, cleanup: cleanupPostgres },
  );
  backendStarted = true;
} catch (error) {
  if (!backendStarted) {
    cleanupPostgres({ force: true });
  }
  throw error;
}
