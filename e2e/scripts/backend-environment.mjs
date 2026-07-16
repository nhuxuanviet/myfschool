const inheritedEnvironmentKeysToRemove = new Set([
  'JAVA_OPTIONS',
  'JAVA_TOOL_OPTIONS',
  'JDK_JAVA_OPTIONS',
  'JVM_ARGS',
  'MAVEN_ARGS',
  'MAVEN_CONFIG',
  'MAVEN_OPTS',
  '_JAVA_OPTIONS',
]);

const inheritedEnvironmentPrefixesToRemove = [
  'APP_',
  'DB_',
  'MANAGEMENT_',
  'POSTGRES_',
  'SERVER_',
  'SPRING_',
];

function shouldRemoveInheritedEnvironmentKey(key) {
  const normalizedKey = key.toUpperCase();
  return (
    inheritedEnvironmentKeysToRemove.has(normalizedKey) ||
    inheritedEnvironmentPrefixesToRemove.some((prefix) =>
      normalizedKey.startsWith(prefix),
    ) ||
    normalizedKey === 'DATABASE_URL'
  );
}

export function createIsolatedBackendEnvironment({
  inheritedEnvironment = process.env,
  postgres,
  apiBaseUrl,
  webBaseUrl,
}) {
  const environment = {};

  for (const [key, value] of Object.entries(inheritedEnvironment)) {
    if (value !== undefined && !shouldRemoveInheritedEnvironmentKey(key)) {
      environment[key] = value;
    }
  }

  const databaseUrl =
    `jdbc:postgresql://127.0.0.1:${postgres.port}/${postgres.database}`;

  Object.assign(environment, {
    SERVER_ADDRESS: apiBaseUrl.hostname,
    SERVER_PORT: apiBaseUrl.port,
    CORS_ALLOWED_ORIGINS: webBaseUrl.origin,
    SPRING_APPLICATION_JSON: JSON.stringify({
      server: {
        address: apiBaseUrl.hostname,
        port: Number(apiBaseUrl.port),
      },
      spring: {
        datasource: {
          url: databaseUrl,
          username: postgres.username,
          password: postgres.password,
        },
        flyway: {
          url: databaseUrl,
          user: postgres.username,
          password: postgres.password,
        },
      },
      app: {
        cors: {
          'allowed-origins': [webBaseUrl.origin],
        },
      },
      management: {
        endpoints: {
          web: {
            cors: {
              'allowed-origins': [webBaseUrl.origin],
            },
          },
        },
      },
    }),
  });

  return environment;
}

export function isBlockedBackendEnvironmentKey(key) {
  return shouldRemoveInheritedEnvironmentKey(key);
}
