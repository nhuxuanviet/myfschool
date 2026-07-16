const loopbackHosts = new Set(['127.0.0.1', 'localhost', '[::1]']);

export function requireLoopbackHttpOrigin(value, variableName) {
  const url = new URL(value);
  const port = Number(url.port);

  if (
    url.protocol !== 'http:' ||
    !loopbackHosts.has(url.hostname) ||
    !Number.isInteger(port) ||
    port < 1 ||
    port > 65_535 ||
    url.username ||
    url.password ||
    url.pathname !== '/' ||
    url.search ||
    url.hash
  ) {
    throw new Error(
      `${variableName} must be a loopback HTTP origin with an explicit valid port and no credentials or path.`,
    );
  }

  return url;
}
