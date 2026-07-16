import {
  expect,
  type APIRequestContext,
  type APIResponse,
} from '@playwright/test';

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const assessmentModeValues = ['NUMERIC', 'REMARK'] as const;
const assessmentKindValues = ['REGULAR', 'MIDTERM', 'FINAL'] as const;
const assessmentFormValues = [
  'ORAL',
  'WRITTEN',
  'PRESENTATION',
  'PRACTICAL',
  'EXPERIMENT',
  'PRODUCT',
  'PROJECT',
] as const;
const assessmentStatusValues = [
  'RECORDED',
  'MAKE_UP_REQUIRED',
  'EXCUSED',
  'ABSENT_FINALIZED',
] as const;
const termResultValues = ['ACHIEVED', 'NOT_ACHIEVED', 'PENDING'] as const;
const assessmentOutcomeValues = ['ACHIEVED', 'NOT_ACHIEVED'] as const;

export type GradeAssessmentMode = (typeof assessmentModeValues)[number];
export type GradeAssessmentKind = (typeof assessmentKindValues)[number];
export type GradeAssessmentForm = (typeof assessmentFormValues)[number];
export type GradeAssessmentStatus = (typeof assessmentStatusValues)[number];
export type GradeTermResult = (typeof termResultValues)[number] | null;
export type GradeAssessmentOutcome =
  | (typeof assessmentOutcomeValues)[number]
  | null;

export interface GradeAcademicTerm {
  id: string;
  academicYear: string;
  code: string;
  name: string;
  startsOn: string;
  endsOn: string;
}

export interface GradeAssessment {
  kind: GradeAssessmentKind;
  form: GradeAssessmentForm;
  displayLabel: string;
  durationMinutes: number | null;
  status: GradeAssessmentStatus;
  score: number | null;
  outcome: GradeAssessmentOutcome;
  assessedOn: string | null;
}

export interface GradeSubject {
  code: string;
  name: string;
  assessmentMode: GradeAssessmentMode;
  annualLessonCount: number | null;
  requiredRegularAssessments: number;
  termAverage: number | null;
  termResult: GradeTermResult;
  assessments: GradeAssessment[];
}

export interface GradesResponse {
  timeZone: string;
  selectedTerm: GradeAcademicTerm;
  availableTerms: GradeAcademicTerm[];
  subjects: GradeSubject[];
}

export class GradesApi {
  constructor(
    private readonly request: APIRequestContext,
    private readonly baseUrl: string,
  ) {}

  get(accessToken: string, termId?: string): Promise<APIResponse> {
    return this.getWithParameters(accessToken, { termId });
  }

  getUnauthenticated(termId?: string): Promise<APIResponse> {
    const url = this.gradesUrl({ termId });
    return this.request.get(url.toString());
  }

  getWithStudentId(
    accessToken: string,
    studentId: string,
    termId?: string,
  ): Promise<APIResponse> {
    return this.getWithParameters(accessToken, { termId, studentId });
  }

  private getWithParameters(
    accessToken: string,
    parameters: { termId?: string; studentId?: string },
  ): Promise<APIResponse> {
    const url = this.gradesUrl(parameters);
    return this.request.get(url.toString(), {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  private gradesUrl(parameters: {
    termId?: string;
    studentId?: string;
  }): URL {
    const url = new URL('/api/v1/grades', this.baseUrl);
    if (parameters.termId !== undefined) {
      url.searchParams.set('termId', parameters.termId);
    }
    if (parameters.studentId !== undefined) {
      url.searchParams.set('studentId', parameters.studentId);
    }
    return url;
  }
}

export async function expectGradesUnauthorized(
  response: APIResponse,
): Promise<void> {
  expect(response.status()).toBe(401);
  expect(response.headers()['content-type']).toContain(
    'application/problem+json',
  );
  expect(await response.json()).toEqual(
    expect.objectContaining({
      status: 401,
      title: expect.any(String),
      detail: expect.any(String),
      code: 'UNAUTHORIZED',
      instance: '/api/v1/grades',
      timestamp: expect.any(String),
    }),
  );
}

export async function expectGradesResponse(
  response: APIResponse,
): Promise<GradesResponse> {
  expect(response.status()).toBe(200);
  expect(response.headers()['content-type']).toContain('application/json');

  const body = (await response.json()) as GradesResponse;
  expect(body).toEqual(
    expect.objectContaining({
      timeZone: 'Asia/Ho_Chi_Minh',
      selectedTerm: expect.any(Object),
      availableTerms: expect.any(Array),
      subjects: expect.any(Array),
    }),
  );

  expectAcademicTerm(body.selectedTerm, 'selectedTerm');
  expect(body.availableTerms).not.toHaveLength(0);
  expectAcademicTermsNewestFirst(body.availableTerms);
  const matchingTerms = body.availableTerms.filter(
    (term) => term.id === body.selectedTerm.id,
  );
  expect(
    matchingTerms,
    'selectedTerm must be present exactly once in availableTerms',
  ).toHaveLength(1);
  expect(
    matchingTerms[0],
    'selectedTerm must deep-equal its matching availableTerms entry',
  ).toEqual(body.selectedTerm);
  expectSubjects(body.subjects);
  return body;
}

export function findAlternativeTerm(
  grades: GradesResponse,
): GradeAcademicTerm {
  const alternative = grades.availableTerms.find(
    (term) => term.id !== grades.selectedTerm.id,
  );
  if (alternative === undefined) {
    throw new Error('Expected the seeded student to have at least two terms.');
  }
  return alternative;
}

export function findNumericSubjectWithAverage(
  grades: GradesResponse,
): GradeSubject {
  const subject = grades.subjects.find((candidate) => {
    if (
      candidate.assessmentMode !== 'NUMERIC' ||
      candidate.termAverage === null
    ) {
      return false;
    }

    return assessmentKindValues.every((kind) =>
      candidate.assessments.some(
        (assessment) =>
          assessment.kind === kind &&
          isFinalizedStatus(assessment.status) &&
          assessment.score !== null,
      ),
    );
  });

  if (subject === undefined) {
    throw new Error(
      'Expected a seeded numeric subject with recorded regular, midterm, and final assessments.',
    );
  }
  return subject;
}

export function findRemarkSubjectWithResult(
  grades: GradesResponse,
): GradeSubject {
  const subject = grades.subjects.find(
    (candidate) =>
      candidate.assessmentMode === 'REMARK' && candidate.termResult !== null,
  );
  if (subject === undefined) {
    throw new Error(
      'Expected a seeded remark subject with a finalized or pending term result.',
    );
  }
  return subject;
}

export function findAssessmentForKind(
  subject: GradeSubject,
  kind: GradeAssessmentKind,
): GradeAssessment {
  const assessment = subject.assessments.find(
    (candidate) => candidate.kind === kind,
  );
  if (assessment === undefined) {
    throw new Error(
      `Expected ${subject.name} to include a ${kind.toLowerCase()} assessment.`,
    );
  }
  return assessment;
}

export function expectCalculatedNumericAverage(subject: GradeSubject): void {
  expect(subject.assessmentMode).toBe('NUMERIC');
  expect(subject.termAverage).not.toBeNull();

  const expectedAverage = roundHalfUp(calculateNumericAverage(subject), 1);
  expect(subject.termAverage).toBe(expectedAverage);
}

export function calculateNumericAverage(subject: GradeSubject): number {
  if (subject.assessmentMode !== 'NUMERIC') {
    throw new Error(`Cannot calculate a numeric average for ${subject.name}.`);
  }

  const finalizedScores = subject.assessments.filter(
    (assessment): assessment is GradeAssessment & { score: number } =>
      isFinalizedStatus(assessment.status) && assessment.score !== null,
  );
  const regularScores = scoresForKind(finalizedScores, 'REGULAR');
  const midtermScores = scoresForKind(finalizedScores, 'MIDTERM');
  const finalScores = scoresForKind(finalizedScores, 'FINAL');

  if (
    regularScores.length === 0 ||
    midtermScores.length === 0 ||
    finalScores.length === 0
  ) {
    throw new Error(
      `Cannot calculate ${subject.name}: recorded regular, midterm, and final scores are required.`,
    );
  }

  if (midtermScores.length !== 1 || finalScores.length !== 1) {
    throw new Error(
      `Cannot calculate ${subject.name}: exactly one recorded midterm and final score are required.`,
    );
  }

  const weightedTotal =
    sum(regularScores) + 2 * midtermScores[0] + 3 * finalScores[0];
  const totalWeight = regularScores.length + 5;
  return weightedTotal / totalWeight;
}

function expectAcademicTermsNewestFirst(terms: GradeAcademicTerm[]): void {
  const seenIds = new Set<string>();
  let previous: GradeAcademicTerm | undefined;

  for (const [index, term] of terms.entries()) {
    const path = `availableTerms[${index}]`;
    expectAcademicTerm(term, path);
    expect(seenIds.has(term.id), `${path}.id must be unique`).toBe(false);
    seenIds.add(term.id);
    if (previous !== undefined) {
      expect(
        compareIsoDates(previous.startsOn, term.startsOn),
        'availableTerms must be ordered newest first by start date',
      ).toBeGreaterThanOrEqual(0);
      if (previous.startsOn === term.startsOn) {
        expect(
          compareIsoDates(previous.endsOn, term.endsOn),
          'availableTerms with equal start dates must be ordered newest first by end date',
        ).toBeGreaterThanOrEqual(0);
      }
    }
    previous = term;
  }
}

function expectAcademicTerm(term: GradeAcademicTerm, path: string): void {
  expect(term).toEqual(
    expect.objectContaining({
      id: expect.any(String),
      academicYear: expect.stringMatching(/^\d{4}-\d{4}$/),
      code: expect.any(String),
      name: expect.any(String),
      startsOn: expect.stringMatching(ISO_DATE_PATTERN),
      endsOn: expect.stringMatching(ISO_DATE_PATTERN),
    }),
  );
  expectNonBlank(term.id, `${path}.id`);
  expectNonBlank(term.code, `${path}.code`);
  expectNonBlank(term.name, `${path}.name`);
  expectIsoDate(term.startsOn, `${path}.startsOn`);
  expectIsoDate(term.endsOn, `${path}.endsOn`);
  expect(
    compareIsoDates(term.startsOn, term.endsOn),
    `${path} must not end before it starts`,
  ).toBeLessThanOrEqual(0);
}

function expectSubjects(subjects: GradeSubject[]): void {
  const subjectCodes = new Set<string>();
  for (const [subjectIndex, subject] of subjects.entries()) {
    const path = `subjects[${subjectIndex}]`;
    expect(subject).toEqual(
      expect.objectContaining({
        code: expect.any(String),
        name: expect.any(String),
        assessmentMode: expect.stringMatching(/^(NUMERIC|REMARK)$/),
        requiredRegularAssessments: expect.any(Number),
        assessments: expect.any(Array),
      }),
    );
    expectNonBlank(subject.code, `${path}.code`);
    expectNonBlank(subject.name, `${path}.name`);
    expect(subjectCodes.has(subject.code), `${path}.code must be unique`).toBe(
      false,
    );
    subjectCodes.add(subject.code);
    expect(assessmentModeValues).toContain(subject.assessmentMode);
    expectAnnualLessonCount(subject, path);
    expectPositiveInteger(
      subject.requiredRegularAssessments,
      `${path}.requiredRegularAssessments`,
    );
    expectTermResult(subject.termResult, `${path}.termResult`);
    expectTermAverage(subject, path);
    expectAssessments(subject, path);
  }
}

function expectTermAverage(subject: GradeSubject, path: string): void {
  expect(
    subject.termAverage === null || typeof subject.termAverage === 'number',
    `${path}.termAverage must be a number or null`,
  ).toBe(true);
  if (subject.termAverage !== null) {
    expect(Number.isFinite(subject.termAverage), `${path}.termAverage`).toBe(
      true,
    );
    expect(subject.termAverage).toBeGreaterThanOrEqual(0);
    expect(subject.termAverage).toBeLessThanOrEqual(10);
  }
  if (subject.assessmentMode === 'REMARK') {
    expect(subject.termAverage, `${path}.termAverage`).toBeNull();
    expect(subject.termResult, `${path}.termResult`).not.toBeNull();
  } else {
    expect(subject.termResult, `${path}.termResult`).toBeNull();
  }
}

function expectAssessments(subject: GradeSubject, subjectPath: string): void {
  for (const [assessmentIndex, assessment] of subject.assessments.entries()) {
    const path = `${subjectPath}.assessments[${assessmentIndex}]`;
    expect(assessment).toEqual(
      expect.objectContaining({
        kind: expect.stringMatching(/^(REGULAR|MIDTERM|FINAL)$/),
        form: expect.stringMatching(
          /^(ORAL|WRITTEN|PRESENTATION|PRACTICAL|EXPERIMENT|PRODUCT|PROJECT)$/,
        ),
        displayLabel: expect.any(String),
        status: expect.stringMatching(
          /^(RECORDED|MAKE_UP_REQUIRED|EXCUSED|ABSENT_FINALIZED)$/,
        ),
      }),
    );
    expect(assessmentKindValues).toContain(assessment.kind);
    expect(assessmentFormValues).toContain(assessment.form);
    expect(assessmentStatusValues).toContain(assessment.status);
    expectNonBlank(assessment.displayLabel, `${path}.displayLabel`);
    expectDurationMinutes(assessment.durationMinutes, `${path}.durationMinutes`);
    expectScore(assessment.score, `${path}.score`);
    expectOutcome(assessment.outcome, `${path}.outcome`);
    expectAssessedOn(assessment.assessedOn, `${path}.assessedOn`);

    if (
      assessment.status === 'MAKE_UP_REQUIRED' ||
      assessment.status === 'EXCUSED'
    ) {
      expect(assessment.score, `${path}.score`).toBeNull();
      expect(assessment.outcome, `${path}.outcome`).toBeNull();
    } else if (subject.assessmentMode === 'NUMERIC') {
      expect(assessment.score, `${path}.score`).not.toBeNull();
      expect(assessment.outcome, `${path}.outcome`).toBeNull();
    } else {
      expect(assessment.score, `${path}.score`).toBeNull();
      expect(assessment.outcome, `${path}.outcome`).not.toBeNull();
    }
  }
}

function expectAnnualLessonCount(subject: GradeSubject, path: string): void {
  const value = subject.annualLessonCount;
  expect(
    value === null || typeof value === 'number',
    `${path}.annualLessonCount must be a number or null`,
  ).toBe(true);

  if (subject.assessmentMode === 'REMARK') {
    expect(value, `${path}.annualLessonCount`).toBeNull();
    return;
  }

  expect(value, `${path}.annualLessonCount`).not.toBeNull();
  if (value !== null) {
    expect(Number.isInteger(value), `${path}.annualLessonCount`).toBe(true);
    expect(value, `${path}.annualLessonCount`).toBeGreaterThanOrEqual(35);
  }
}

function expectDurationMinutes(value: number | null, path: string): void {
  expect(value === null || typeof value === 'number', `${path} must be a number or null`).toBe(
    true,
  );
  if (value !== null) {
    expect(Number.isInteger(value), path).toBe(true);
    expect(value).toBeGreaterThan(0);
  }
}

function expectScore(value: number | null, path: string): void {
  expect(value === null || typeof value === 'number', `${path} must be a number or null`).toBe(
    true,
  );
  if (value !== null) {
    expect(Number.isFinite(value), path).toBe(true);
    expect(value).toBeGreaterThanOrEqual(0);
    expect(value).toBeLessThanOrEqual(10);
  }
}

function expectOutcome(value: GradeAssessmentOutcome, path: string): void {
  expect(value === null || assessmentOutcomeValues.includes(value), path).toBe(
    true,
  );
}

function expectAssessedOn(value: string | null, path: string): void {
  expect(value === null || typeof value === 'string', `${path} must be an ISO date or null`).toBe(
    true,
  );
  if (value !== null) {
    expectIsoDate(value, path);
  }
}

function expectTermResult(value: GradeTermResult, path: string): void {
  expect(value === null || termResultValues.includes(value), path).toBe(true);
}

function scoresForKind(
  assessments: Array<GradeAssessment & { score: number }>,
  kind: GradeAssessmentKind,
): number[] {
  return assessments
    .filter((assessment) => assessment.kind === kind)
    .map((assessment) => assessment.score);
}

function isFinalizedStatus(status: GradeAssessmentStatus): boolean {
  return status === 'RECORDED' || status === 'ABSENT_FINALIZED';
}

function roundHalfUp(value: number, decimalPlaces: number): number {
  const multiplier = 10 ** decimalPlaces;
  return Math.floor((value + Number.EPSILON) * multiplier + 0.5) / multiplier;
}

function sum(values: number[]): number {
  return values.reduce((total, value) => total + value, 0);
}

function expectIsoDate(value: string, path: string): void {
  expect(ISO_DATE_PATTERN.test(value), path).toBe(true);
  const [year, month, day] = value.split('-').map(Number);
  const parsed = new Date(Date.UTC(year, month - 1, day));
  expect(parsed.toISOString().slice(0, 10), path).toBe(value);
}

function expectNonBlank(value: string, path: string): void {
  expect(value.trim(), path).not.toBe('');
}

function expectPositiveInteger(value: number, path: string): void {
  expect(Number.isInteger(value), path).toBe(true);
  expect(value, path).toBeGreaterThan(0);
}

function compareIsoDates(left: string, right: string): number {
  if (left === right) return 0;
  return left < right ? -1 : 1;
}
