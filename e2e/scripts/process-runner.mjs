import { spawnSync } from 'node:child_process';

import crossSpawn from 'cross-spawn';

const isWindows = process.platform === 'win32';

export function runCommand(
  command,
  args,
  { cwd, env = process.env, quiet = false, allowFailure = false } = {},
) {
  const result = crossSpawn.sync(command, args, {
    cwd,
    env,
    encoding: 'utf8',
    windowsHide: true,
  });

  if (result.error) {
    throw new Error(
      `Unable to run ${command}: ${result.error.message}`,
      { cause: result.error },
    );
  }

  if (!quiet) {
    if (result.stdout) process.stdout.write(result.stdout);
    if (result.stderr) process.stderr.write(result.stderr);
  }

  if (!allowFailure && result.status !== 0) {
    const output = [result.stdout, result.stderr].filter(Boolean).join('\n').trim();
    throw new Error(
      `${command} exited with code ${result.status}${output ? `:\n${output}` : ''}`,
    );
  }

  return result;
}

function wait(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function terminateProcessTree(child, signal) {
  if (!child.pid) return;

  if (isWindows) {
    spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], {
      stdio: 'ignore',
      windowsHide: true,
    });
    return;
  }

  try {
    process.kill(-child.pid, signal);
  } catch (error) {
    if (error.code === 'ESRCH') return;
    try {
      child.kill(signal);
    } catch (childError) {
      if (childError.code !== 'ESRCH') throw childError;
    }
  }
}

export function spawnLongRunningProcess(
  command,
  args,
  { cwd, env = process.env, stdio = 'inherit' } = {},
) {
  const child = crossSpawn(command, args, {
    cwd,
    env,
    stdio,
    windowsHide: true,
    detached: !isWindows,
  });

  let result;
  let resolveExit;
  const exited = new Promise((resolve) => {
    resolveExit = resolve;
  });
  const settle = (nextResult) => {
    if (result) return;
    result = nextResult;
    resolveExit(result);
  };

  child.once('error', (error) => settle({ code: undefined, error }));
  child.once('exit', (code, signal) => settle({ code, signal }));

  return {
    child,
    exited,
    get hasExited() {
      return result !== undefined;
    },
    get result() {
      return result;
    },
    async stop({ gracePeriodMs = 5_000 } = {}) {
      if (result) return result;

      terminateProcessTree(child, 'SIGTERM');
      await Promise.race([exited, wait(gracePeriodMs)]);

      if (!result) {
        terminateProcessTree(child, 'SIGKILL');
        await Promise.race([exited, wait(gracePeriodMs)]);
      }

      if (!result) {
        throw new Error(
          `${command} did not terminate after the E2E runner requested cleanup.`,
        );
      }

      return exited;
    },
  };
}

export function startLongRunningProcess(command, args, options = {}) {
  const { cleanup, ...spawnOptions } = options;
  const running = spawnLongRunningProcess(command, args, spawnOptions);
  let stopping = false;
  let cleaned = false;

  const runCleanup = () => {
    if (cleaned || !cleanup) return;
    cleaned = true;
    try {
      cleanup();
    } catch (error) {
      console.error('Long-running process cleanup failed:', error);
    }
  };

  const stop = async () => {
    if (stopping) return;
    stopping = true;
    try {
      await running.stop();
    } catch (error) {
      console.error(`Unable to stop ${command}:`, error);
    } finally {
      runCleanup();
      process.exit(0);
    }
  };

  process.once('SIGINT', () => void stop());
  process.once('SIGTERM', () => void stop());

  void running.exited.then((result) => {
    if (result.error) {
      console.error(`Unable to start ${command}:`, result.error);
    } else if (!stopping && result.signal) {
      console.error(`${command} stopped by signal ${result.signal}.`);
    }
    runCleanup();
    process.exit(stopping ? 0 : (result.code ?? 1));
  });

  return running;
}
