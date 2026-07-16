import assert from 'node:assert/strict';

import {
  createIsolatedBackendEnvironment,
  isBlockedBackendEnvironmentKey,
} from './backend-environment.mjs';

const inheritedEnvironment = {
  HOME: '/safe/home',
  JAVA_TOOL_OPTIONS: '-Dspring.datasource.url=jdbc:postgresql://forbidden/java',
  JDK_JAVA_OPTIONS: '-Dspring.flyway.url=jdbc:postgresql://forbidden/jdk',
  MAVEN_ARGS:
    '-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://forbidden/args',
  MAVEN_OPTS:
    '-Dspring-boot.run.jvmArguments=-Dspring.datasource.url=jdbc:postgresql://forbidden/opts',
  mAvEn_CoNfIg: '-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://forbidden/config',
  SPRING_APPLICATION_JSON:
    '{"spring":{"datasource":{"url":"jdbc:postgresql://forbidden/json"}}}',
  SPRING_DATASOURCE_URL: 'jdbc:postgresql://forbidden/environment',
};

const environment = createIsolatedBackendEnvironment({
  inheritedEnvironment,
  postgres: {
    database: 'myschool',
    password: 'isolated-password',
    port: 54321,
    username: 'myschool',
  },
  apiBaseUrl: new URL('http://127.0.0.1:8080'),
  webBaseUrl: new URL('http://127.0.0.1:4173'),
});

assert.equal(environment.HOME, '/safe/home');
for (const key of Object.keys(inheritedEnvironment)) {
  if (
    isBlockedBackendEnvironmentKey(key) &&
    key.toUpperCase() !== 'SPRING_APPLICATION_JSON'
  ) {
    assert.equal(environment[key], undefined, `${key} must not reach Maven`);
  }
}

const applicationJson = JSON.parse(environment.SPRING_APPLICATION_JSON);
assert.equal(
  applicationJson.spring.datasource.url,
  'jdbc:postgresql://127.0.0.1:54321/myschool',
);
assert.equal(
  applicationJson.spring.flyway.url,
  'jdbc:postgresql://127.0.0.1:54321/myschool',
);
assert.equal(applicationJson.server.port, 8080);

console.log('Backend E2E environment isolation checks passed.');
