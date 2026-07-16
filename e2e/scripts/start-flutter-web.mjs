import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

import { requireLoopbackHttpOrigin } from './origin.mjs';
import { projectRoot, requirePath } from './project-paths.mjs';
import { runCommand } from './process-runner.mjs';

const contentTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.ico', 'image/x-icon'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml'],
  ['.wasm', 'application/wasm'],
]);

export function buildFlutterWeb({ environment = process.env } = {}) {
  requirePath(path.join(projectRoot, 'pubspec.yaml'), 'Flutter pubspec.yaml');

  const apiBaseUrl = requireLoopbackHttpOrigin(
    environment.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080',
    'E2E_API_BASE_URL',
  ).origin;
  const webBaseUrl = requireLoopbackHttpOrigin(
    environment.E2E_WEB_BASE_URL ?? 'http://127.0.0.1:4173',
    'E2E_WEB_BASE_URL',
  );
  const webRoot = path.join(projectRoot, 'build', 'web');

  console.log('Building Flutter Web for Playwright...');
  const flutterArgs = [
    'build',
    'web',
    '--no-wasm-dry-run',
    `--dart-define=API_BASE_URL=${apiBaseUrl}`,
  ];
  const buildCommand =
    process.platform === 'win32'
      ? {
          command: 'cmd.exe',
          args: ['/d', '/s', '/c', 'flutter', ...flutterArgs],
        }
      : { command: 'flutter', args: flutterArgs };
  runCommand(buildCommand.command, buildCommand.args, {
    cwd: projectRoot,
    env: environment,
  });

  requirePath(path.join(webRoot, 'index.html'), 'Flutter Web build');
  return { webBaseUrl, webRoot };
}

function resolveAsset(requestUrl, { webBaseUrl, webRoot }) {
  const pathname = decodeURIComponent(
    new URL(requestUrl ?? '/', webBaseUrl).pathname,
  );
  const relativePath = pathname === '/' ? 'index.html' : pathname.slice(1);
  const candidate = path.resolve(webRoot, relativePath);
  const root = path.resolve(webRoot);

  if (candidate !== root && !candidate.startsWith(`${root}${path.sep}`)) {
    return undefined;
  }
  if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
    return candidate;
  }

  return path.join(webRoot, 'index.html');
}

export async function startFlutterWebServer({ webBaseUrl, webRoot }) {
  const server = http.createServer((request, response) => {
    const assetPath = resolveAsset(request.url, { webBaseUrl, webRoot });
    if (!assetPath) {
      response.writeHead(400).end('Bad request');
      return;
    }

    response.writeHead(200, {
      'Cache-Control': 'no-store',
      'Content-Type':
        contentTypes.get(path.extname(assetPath).toLowerCase()) ??
        'application/octet-stream',
    });
    fs.createReadStream(assetPath)
      .on('error', () => response.destroy())
      .pipe(response);
  });

  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(Number(webBaseUrl.port), webBaseUrl.hostname, resolve);
  });
  console.log(`Serving Flutter Web at ${webBaseUrl.origin}`);

  return {
    close: () =>
      new Promise((resolve, reject) => {
        server.close((error) => (error ? reject(error) : resolve()));
      }),
  };
}

async function runStandaloneServer() {
  const build = buildFlutterWeb();
  const server = await startFlutterWebServer(build);
  let stopPromise;
  const stop = () => {
    stopPromise ??= server.close();
    return stopPromise;
  };

  await new Promise((resolve) => {
    for (const signal of ['SIGINT', 'SIGTERM']) {
      process.once(signal, () => void stop().finally(resolve));
    }
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await runStandaloneServer();
}
