import { setTimeout as delay } from 'node:timers/promises';
import { pathToFileURL } from 'node:url';

import {
  composeFile,
  projectRoot,
  requireE2eComposeProjectName,
  requirePath,
} from './project-paths.mjs';
import { runCommand } from './process-runner.mjs';

const composeProjectName = requireE2eComposeProjectName(
  process.env.E2E_COMPOSE_PROJECT_NAME ?? `myschool-e2e-manual-${process.pid}`,
);
const composePrefix = [
  'compose',
  '--project-name',
  composeProjectName,
  '--file',
  composeFile,
];

function existingComposePort() {
  const runningContainer = runCommand(
    'docker',
    [...composePrefix, 'ps', '--status', 'running', '--quiet', 'postgres'],
    { cwd: projectRoot, quiet: true, allowFailure: true },
  );
  if (runningContainer.status !== 0 || !runningContainer.stdout?.trim()) {
    return undefined;
  }

  const result = runCommand(
    'docker',
    [...composePrefix, 'port', 'postgres', '5432'],
    { cwd: projectRoot, quiet: true, allowFailure: true },
  );
  if (result.status !== 0) return undefined;

  const match = result.stdout?.trim().match(/:(\d+)$/);
  return match ? Number(match[1]) : undefined;
}

function requestedPostgresPort() {
  const rawPort = process.env.E2E_POSTGRES_PORT ?? process.env.POSTGRES_PORT ?? '0';
  const port = Number(rawPort);
  if (!Number.isInteger(port) || port < 0 || port > 65_535) {
    throw new Error('E2E_POSTGRES_PORT must be an integer from 0 to 65535.');
  }
  return port;
}

export function cleanupPostgres({ force = false } = {}) {
  if (!force && process.env.E2E_KEEP_DATABASE === 'true') return;
  runCommand(
    'docker',
    [...composePrefix, 'down', '--volumes', '--remove-orphans'],
    { cwd: projectRoot, quiet: true },
  );
}

export async function preparePostgres() {
  requirePath(composeFile, 'Root compose.yaml');

  cleanupPostgres();
  let startupAttempted = false;

  try {
    const requestedPort = requestedPostgresPort();
    const database = process.env.DB_NAME ?? process.env.POSTGRES_DB ?? 'myschool';
    const username =
      process.env.DB_USERNAME ?? process.env.POSTGRES_USER ?? 'myschool';
    const password =
      process.env.DB_PASSWORD ?? process.env.POSTGRES_PASSWORD ?? 'myschool';
    const composeEnvironment = {
      ...process.env,
      DB_NAME: database,
      DB_USERNAME: username,
      DB_PASSWORD: password,
      POSTGRES_PORT: String(requestedPort),
    };

    console.log('Starting the PostgreSQL E2E dependency from root compose.yaml...');
    const waitTimeout = process.env.E2E_COMPOSE_WAIT_TIMEOUT_SECONDS ?? '120';
    const waitTimeoutSeconds = Number(waitTimeout);
    if (!Number.isInteger(waitTimeoutSeconds) || waitTimeoutSeconds < 1) {
      throw new Error(
        'E2E_COMPOSE_WAIT_TIMEOUT_SECONDS must be a positive integer.',
      );
    }

    const composeArgs = [
      ...composePrefix,
      'up',
      '--detach',
      '--wait',
      '--wait-timeout',
      waitTimeout,
      'postgres',
    ];
    startupAttempted = true;
    const startResult = runCommand('docker', composeArgs, {
      cwd: projectRoot,
      env: composeEnvironment,
      allowFailure: true,
    });

    if (startResult.status !== 0) {
      const output = `${startResult.stdout ?? ''}\n${startResult.stderr ?? ''}`;
      if (!/unknown (flag|option).*wait/i.test(output)) {
        throw new Error('Docker Compose could not start the postgres service.');
      }

      console.warn('Docker Compose does not support --wait; polling pg_isready instead.');
      runCommand(
        'docker',
        [...composePrefix, 'up', '--detach', 'postgres'],
        { cwd: projectRoot, env: composeEnvironment },
      );
    }

    const readyArgs = [
      ...composePrefix,
      'exec',
      '--no-TTY',
      'postgres',
      'pg_isready',
      '--username',
      username,
      '--dbname',
      database,
    ];
    const readyDeadline = Date.now() + waitTimeoutSeconds * 1_000;

    while (Date.now() < readyDeadline) {
      const result = runCommand('docker', readyArgs, {
        cwd: projectRoot,
        env: composeEnvironment,
        quiet: true,
        allowFailure: true,
      });
      if (result.status === 0) {
        const postgresPort = existingComposePort();
        if (!postgresPort) {
          throw new Error('Docker did not publish the PostgreSQL E2E port.');
        }
        console.log('PostgreSQL is ready for E2E tests.');
        return {
          database,
          password,
          port: postgresPort,
          username,
        };
      }
      await delay(1_000);
    }

    throw new Error(`PostgreSQL was not ready within ${waitTimeout} seconds.`);
  } catch (error) {
    if (!startupAttempted) throw error;

    try {
      cleanupPostgres({ force: true });
    } catch (cleanupError) {
      throw new AggregateError(
        [error, cleanupError],
        'PostgreSQL E2E startup failed and its partial Compose resources could not be removed.',
      );
    }

    throw error;
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await preparePostgres();
}
