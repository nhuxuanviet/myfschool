import { expect, test, type Locator, type Page } from '@playwright/test';

import {
  AuthApi,
  expectTokenResponse,
  seededStudent,
} from './support/auth-api.js';
import {
  expectCalculatedNumericAverage,
  expectGradesResponse,
  expectGradesUnauthorized,
  findAlternativeTerm,
  findAssessmentForKind,
  findNumericSubjectWithAverage,
  findRemarkSubjectWithResult,
  type GradeAcademicTerm,
  type GradeSubject,
  GradesApi,
} from './support/grades-api.js';
import {
  enableFlutterSemantics,
  gradesPage,
  homePage,
  loginPage,
} from './support/flutter-semantics.js';

const apiBaseUrl = process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080';
const studentIdProbe = '00000000-0000-0000-0000-000000000999';
const assessmentKinds = ['REGULAR', 'MIDTERM', 'FINAL'] as const;

async function enterFlutterText(field: Locator, value: string): Promise<void> {
  await field.click();
  await field.press('ControlOrMeta+A');
  await field.pressSequentially(value);
  await expect(field).toHaveValue(value);
}

async function loginThroughUi(page: Page): Promise<void> {
  const login = loginPage(page);
  await enterFlutterText(login.phone, seededStudent.phoneNumber);
  await enterFlutterText(login.password, seededStudent.password);
  await login.loginButton.click();
  await expect(homePage(page).screen).toBeVisible();
  await expect(page).toHaveURL(/\/home$/);
}

async function findAlternativeTermWithAssessmentDetails(
  gradesApi: GradesApi,
  accessToken: string,
  initialTerm: GradeAcademicTerm,
  alternativeTerm: GradeAcademicTerm,
): Promise<{ subject: GradeSubject }> {
  const grades = await expectGradesResponse(
    await gradesApi.get(accessToken, alternativeTerm.id),
  );
  expect(grades.selectedTerm).toEqual(alternativeTerm);
  expect(grades.selectedTerm.id).not.toBe(initialTerm.id);

  const subject = grades.subjects.find((candidate) =>
    assessmentKinds.every((kind) =>
      candidate.assessments.some((assessment) => assessment.kind === kind),
    ),
  );
  if (subject === undefined) {
    throw new Error(
      'Expected a switched academic term with regular, midterm, and final assessment details.',
    );
  }

  return { subject };
}

async function expectAssessmentDetails(
  page: Page,
  subject: GradeSubject,
): Promise<void> {
  const grades = gradesPage(page);
  await expect(grades.detailScreen(subject)).toBeVisible();
  for (const kind of assessmentKinds) {
    const section = grades.assessmentSection(kind);
    await expect(section).toBeVisible();
    const assessment = grades.assessment(findAssessmentForKind(subject, kind));
    await expect(assessment).toBeVisible();
  }
}

test.describe('Phase 05 semester grades under Circular 22/2021', () => {
  test('requires a student token and returns scoped grade calculations for each term', async ({
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const gradesApi = new GradesApi(request, apiBaseUrl);

    await expectGradesUnauthorized(await gradesApi.getUnauthenticated());

    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const grades = await expectGradesResponse(
      await gradesApi.get(tokenResponse.accessToken),
    );
    expect(grades.subjects).not.toHaveLength(0);
    expect(grades).not.toHaveProperty('student');
    expect(grades).not.toHaveProperty('studentId');

    const numericSubject = findNumericSubjectWithAverage(grades);
    expectCalculatedNumericAverage(numericSubject);
    for (const kind of assessmentKinds) {
      const assessment = findAssessmentForKind(numericSubject, kind);
      expect(assessment.kind).toBe(kind);
      expect(assessment.displayLabel.trim()).not.toBe('');
    }

    const remarkSubject = findRemarkSubjectWithResult(grades);
    expect(remarkSubject.termAverage).toBeNull();
    expect(remarkSubject.termResult).toMatch(
      /^(ACHIEVED|NOT_ACHIEVED|PENDING)$/,
    );
    for (const assessment of remarkSubject.assessments) {
      expect(assessment.score).toBeNull();
    }

    const alternativeTerm = findAlternativeTerm(grades);
    const gradesForAlternativeTerm = await expectGradesResponse(
      await gradesApi.get(tokenResponse.accessToken, alternativeTerm.id),
    );
    expect(gradesForAlternativeTerm.selectedTerm).toEqual(alternativeTerm);
    expect(gradesForAlternativeTerm.availableTerms).toEqual(grades.availableTerms);
    expect(gradesForAlternativeTerm.subjects).not.toHaveLength(0);

    const clientSuppliedStudent = await expectGradesResponse(
      await gradesApi.getWithStudentId(
        tokenResponse.accessToken,
        studentIdProbe,
      ),
    );
    expect(clientSuppliedStudent.selectedTerm).toEqual(grades.selectedTerm);
    expect(clientSuppliedStudent.availableTerms).toEqual(grades.availableTerms);
    expect(clientSuppliedStudent.subjects).toEqual(grades.subjects);
  });

  test('switches terms and opens real subject assessment details through Flutter', async ({
    page,
    request,
  }) => {
    const auth = new AuthApi(request, apiBaseUrl);
    const gradesApi = new GradesApi(request, apiBaseUrl);
    const tokenResponse = await expectTokenResponse(
      await auth.login(seededStudent.password),
    );
    const initialGrades = await expectGradesResponse(
      await gradesApi.get(tokenResponse.accessToken),
    );
    const alternativeTerm = findAlternativeTerm(initialGrades);
    const { subject } = await findAlternativeTermWithAssessmentDetails(
      gradesApi,
      tokenResponse.accessToken,
      initialGrades.selectedTerm,
      alternativeTerm,
    );

    await page.goto('/login');
    await enableFlutterSemantics(page);
    await loginThroughUi(page);

    await homePage(page).quickAction('Kết quả').click();
    await expect(page).toHaveURL(/\/grades$/);

    const grades = gradesPage(page);
    await expect(grades.screen).toBeVisible();
    await expect(grades.termControl(initialGrades.selectedTerm)).toBeVisible();
    await grades.termControl(initialGrades.selectedTerm).click();
    await expect(grades.termOption(alternativeTerm)).toBeVisible();
    await grades.termOption(alternativeTerm).click();
    await expect(grades.termControl(alternativeTerm)).toBeVisible();

    await expect(grades.subjectSummary(subject)).toBeVisible();
    await grades.subjectDetailsButton(subject).click();
    const expectedDetailHash =
      `#/grades/subjects/${encodeURIComponent(subject.code)}` +
      `?termId=${encodeURIComponent(alternativeTerm.id)}`;
    await expect(page).toHaveURL((url) => url.hash === expectedDetailHash);
    await expectAssessmentDetails(page, subject);

    await page.reload();
    await enableFlutterSemantics(page, gradesPage(page).detailScreen(subject));
    await expect(page).toHaveURL((url) => url.hash === expectedDetailHash);
    await expectAssessmentDetails(page, subject);
  });
});
