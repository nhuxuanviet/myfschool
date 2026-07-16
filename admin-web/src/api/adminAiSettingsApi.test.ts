import { afterEach, describe, expect, it, vi } from 'vitest';
import { getAdminAiSettings, updateAdminAiSettings } from './adminAiSettingsApi';

describe('adminAiSettingsApi', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('reads only safe provider readiness and runtime fields', async () => {
    const responseBody = {
      provider: 'OPENAI',
      status: 'READY',
      apiKeyConfigured: true,
      model: 'gpt-5.6-luna',
      temperature: 0.6,
      maxCompletionTokens: 800,
      memoryMaxMessages: 12,
      version: 1,
      updatedAt: '2026-07-15T00:00:00Z',
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(responseBody), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(getAdminAiSettings('token')).resolves.toEqual(responseBody);
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/ai/settings', {
      headers: { Authorization: 'Bearer token' },
    });
  });

  it('sends optimistic-lock version when updating runtime settings', async () => {
    const input = {
      model: 'gpt-5.6-luna', temperature: 0.7, maxCompletionTokens: 900,
      memoryMaxMessages: 14, version: 2,
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...input, provider: 'OPENAI' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);

    await updateAdminAiSettings('token', input);
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/ai/settings', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify(input),
    }));
  });
});
