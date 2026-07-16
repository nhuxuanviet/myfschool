import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const e2eRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
);
export const projectRoot = path.resolve(e2eRoot, '..');
export const backendRoot = path.join(projectRoot, 'backend');
export const composeFile = path.join(projectRoot, 'compose.yaml');

export function requireE2eComposeProjectName(value) {
  if (!/^myschool-e2e(?:-[a-z0-9][a-z0-9_-]*)?$/.test(value)) {
    throw new Error(
      'E2E_COMPOSE_PROJECT_NAME must use the reserved myschool-e2e-* prefix.',
    );
  }
  return value;
}

export function requirePath(targetPath, description) {
  if (!fs.existsSync(targetPath)) {
    throw new Error(`${description} was not found at ${targetPath}`);
  }
}
