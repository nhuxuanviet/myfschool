import { parseResponse } from './adminAuthApi';

export interface AdminAiSettings {
  provider: 'OPENAI' | 'LOCAL' | string;
  status: 'READY' | 'MISSING_API_KEY' | 'LOCAL_FALLBACK' | 'UNSUPPORTED_PROVIDER';
  apiKeyConfigured: boolean;
  model: string;
  temperature: number;
  maxCompletionTokens: number;
  memoryMaxMessages: number;
  version: number;
  updatedAt: string;
}

export interface UpdateAdminAiSettings {
  model: string;
  temperature: number;
  maxCompletionTokens: number;
  memoryMaxMessages: number;
  version: number;
}

export async function getAdminAiSettings(accessToken: string): Promise<AdminAiSettings> {
  return parseResponse(await fetch('/api/v1/admin/ai/settings', {
    headers: { Authorization: `Bearer ${accessToken}` },
  }));
}

export async function updateAdminAiSettings(
  accessToken: string,
  input: UpdateAdminAiSettings,
): Promise<AdminAiSettings> {
  return parseResponse(await fetch('/api/v1/admin/ai/settings', {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(input),
  }));
}
