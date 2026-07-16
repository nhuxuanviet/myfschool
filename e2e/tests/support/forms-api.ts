import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';

const formTypes = [
  'LEAVE_OF_ABSENCE',
  'STUDENT_CONFIRMATION',
  'TRANSCRIPT_REQUEST',
  'STUDENT_CARD_REISSUE',
] as const;
const formStatuses = [
  'SUBMITTED',
  'IN_REVIEW',
  'APPROVED',
  'REJECTED',
  'CANCELLED',
] as const;

export type StudentFormType = (typeof formTypes)[number];
export type StudentFormStatus = (typeof formStatuses)[number];

export interface StudentFormSummary {
  id: string;
  type: StudentFormType;
  startsOn: string | null;
  endsOn: string | null;
  status: StudentFormStatus;
  submittedAt: string;
  updatedAt: string;
  canCancel: boolean;
}

export interface StudentFormDetails extends StudentFormSummary {
  reason: string;
  timeline: Array<{
    id: string;
    status: StudentFormStatus;
    occurredAt: string;
    note: string | null;
  }>;
}

export interface StudentFormsResponse {
  forms: StudentFormSummary[];
}

export class StudentFormsApi {
  constructor(
    private readonly request: APIRequestContext,
    private readonly baseUrl: string,
  ) {}

  list(accessToken: string, status?: StudentFormStatus): Promise<APIResponse> {
    return this.listWithParameters(accessToken, status === undefined ? {} : { status });
  }

  listUnauthenticated(): Promise<APIResponse> {
    return this.request.get(new URL('/api/v1/forms', this.baseUrl).toString());
  }

  listWithStudentId(accessToken: string, studentId: string): Promise<APIResponse> {
    return this.listWithParameters(accessToken, { studentId });
  }

  get(accessToken: string, formId: string): Promise<APIResponse> {
    return this.request.get(this.formUrl(formId).toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  create(
    accessToken: string,
    payload: {
      type: StudentFormType;
      reason: string;
      startsOn?: string | null;
      endsOn?: string | null;
    },
  ): Promise<APIResponse> {
    return this.request.post(new URL('/api/v1/forms', this.baseUrl).toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
      data: {
        ...payload,
        startsOn: payload.startsOn ?? null,
        endsOn: payload.endsOn ?? null,
      },
    });
  }

  cancel(accessToken: string, formId: string): Promise<APIResponse> {
    return this.request.delete(this.formUrl(formId).toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  private listWithParameters(
    accessToken: string,
    parameters: { status?: StudentFormStatus; studentId?: string },
  ): Promise<APIResponse> {
    const url = new URL('/api/v1/forms', this.baseUrl);
    if (parameters.status !== undefined) url.searchParams.set('status', parameters.status);
    if (parameters.studentId !== undefined) url.searchParams.set('studentId', parameters.studentId);
    return this.request.get(url.toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  private formUrl(formId: string): URL {
    return new URL(`/api/v1/forms/${encodeURIComponent(formId)}`, this.baseUrl);
  }
}

export async function expectFormsResponse(response: APIResponse): Promise<StudentFormsResponse> {
  expect(response.status()).toBe(200);
  expect(response.headers()['content-type']).toContain('application/json');
  const body = (await response.json()) as StudentFormsResponse;
  expect(Object.keys(body).sort()).toEqual(['forms']);
  expect(Array.isArray(body.forms)).toBe(true);
  const ids = new Set<string>();
  for (const form of body.forms) {
    expectFormSummary(form);
    expect(ids.has(form.id)).toBe(false);
    ids.add(form.id);
  }
  return body;
}

export async function expectFormDetails(
  response: APIResponse,
  expectedStatus: 200 | 201 = 200,
): Promise<StudentFormDetails> {
  expect(response.status()).toBe(expectedStatus);
  expect(response.headers()['content-type']).toContain('application/json');
  const form = (await response.json()) as StudentFormDetails;
  expectFormSummary(form);
  expect(form.reason.trim()).not.toBe('');
  expect(Array.isArray(form.timeline)).toBe(true);
  expect(form.timeline.length).toBeGreaterThan(0);
  let previous = 0;
  for (const entry of form.timeline) {
    expectUuid(entry.id);
    expect(formStatuses).toContain(entry.status);
    const occurredAt = instant(entry.occurredAt);
    expect(occurredAt).toBeGreaterThanOrEqual(previous);
    previous = occurredAt;
    expect(entry.note === null || entry.note.trim() !== '').toBe(true);
  }
  expect(form.timeline.at(-1)?.status).toBe(form.status);
  return form;
}

export async function expectFormNotFound(response: APIResponse, formId: string): Promise<void> {
  expect(response.status()).toBe(404);
  expect(response.headers()['content-type']).toContain('application/problem+json');
  expect(await response.json()).toEqual(
    expect.objectContaining({
      code: 'FORM_NOT_FOUND',
      status: 404,
      instance: `/api/v1/forms/${formId}`,
    }),
  );
}

function expectFormSummary(form: StudentFormSummary): void {
  expectUuid(form.id);
  expect(formTypes).toContain(form.type);
  expect(formStatuses).toContain(form.status);
  instant(form.submittedAt);
  expect(instant(form.updatedAt)).toBeGreaterThanOrEqual(instant(form.submittedAt));
  expect(typeof form.canCancel).toBe('boolean');
  expect(form.canCancel).toBe(form.status === 'SUBMITTED' || form.status === 'IN_REVIEW');
  if (form.type === 'LEAVE_OF_ABSENCE') {
    expect(form.startsOn).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(form.endsOn).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(Date.parse(`${form.endsOn}T00:00:00Z`)).toBeGreaterThanOrEqual(
      Date.parse(`${form.startsOn}T00:00:00Z`),
    );
  } else {
    expect(form.startsOn).toBeNull();
    expect(form.endsOn).toBeNull();
  }
}

function expectUuid(value: string): void {
  expect(value).toMatch(
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  );
}

function instant(value: string): number {
  expect(value).toMatch(
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/,
  );
  const parsed = Date.parse(value);
  expect(Number.isNaN(parsed)).toBe(false);
  return parsed;
}
