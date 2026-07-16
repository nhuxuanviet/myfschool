import { expect, type Locator, type Page } from '@playwright/test';

import type {
  TimetableLesson,
  TimetableLessonOccurrence,
} from './timetable-api.js';
import type {
  GradeAcademicTerm,
  GradeAssessment,
  GradeSubject,
} from './grades-api.js';
import type { EventCategory, SchoolEvent } from './events-api.js';
import type {
  StudentFormDetails,
  StudentFormSummary,
  StudentFormType,
} from './forms-api.js';
import type { ClubCategory, SchoolClub } from './clubs-api.js';

const labels = {
  loginScreen: 'Màn hình đăng nhập',
  loginLogo: 'FPT Schools',
  phone: 'Số điện thoại',
  password: 'Mật khẩu',
  login: 'Đăng nhập',
  forgotPassword: 'Quên mật khẩu?',
  resetHeading: 'Đặt lại mật khẩu',
  requestOtp: 'Gửi mã OTP',
  otp: 'Mã OTP',
  verifyOtp: 'Xác minh OTP',
  newPassword: 'Mật khẩu mới',
  confirmPassword: 'Xác nhận mật khẩu mới',
  confirmReset: 'Xác nhận',
  completeReset: 'Đặt lại mật khẩu',
  backToLogin: 'Quay lại đăng nhập',
  homeScreen: 'Trang chủ',
  logout: 'Đăng xuất',
  systemOnline: 'Kết nối hệ thống: trực tuyến',
  gradesScreen: 'Điểm học kỳ',
  eventsScreen: 'Sự kiện',
  eventsFilter: 'Lọc sự kiện',
  includePastEvents: 'Hiển thị sự kiện đã qua',
  registerEvent: 'Đăng ký sự kiện',
  cancelEvent: 'Hủy đăng ký',
  formsScreen: 'Đơn từ',
  createForm: 'Tạo đơn mới',
  formType: 'Loại đơn',
  submitForm: 'Gửi đơn',
  cancelForm: 'Hủy đơn',
  confirmCancelForm: 'Xác nhận hủy đơn',
} as const;

export async function enableFlutterSemantics(
  page: Page,
  readyScreen: Locator = loginPage(page).screen,
): Promise<void> {
  await page.waitForLoadState('domcontentloaded');

  const enableAccessibility = page.getByRole('button', {
    name: 'Enable accessibility',
    exact: true,
  });
  const placeholder = page.locator('flt-semantics-placeholder');

  await expect(
    enableAccessibility.or(readyScreen).or(placeholder).first(),
  ).toBeVisible({ timeout: 30_000 });

  if (await enableAccessibility.isVisible()) {
    await enableAccessibility.evaluate((element: HTMLElement) => element.click());
  } else if (
    (await placeholder.count()) > 0 &&
    (await placeholder.first().isVisible())
  ) {
    await placeholder
      .first()
      .evaluate((element: HTMLElement) => element.click());
  }

  await expect(readyScreen).toBeVisible({ timeout: 30_000 });
}

export function loginPage(page: Page): {
  screen: Locator;
  logo: Locator;
  phone: Locator;
  password: Locator;
  loginButton: Locator;
  forgotPasswordButton: Locator;
  systemOnline: Locator;
} {
  return {
    screen: page.getByLabel(labels.loginScreen, { exact: true }),
    logo: page.getByRole('img', { name: labels.loginLogo, exact: true }),
    phone: page.getByRole('textbox', { name: labels.phone, exact: true }),
    password: page.getByLabel(labels.password, { exact: true }),
    loginButton: page.getByRole('button', { name: labels.login, exact: true }),
    forgotPasswordButton: page.getByRole('button', {
      name: labels.forgotPassword,
      exact: true,
    }),
    systemOnline: page.getByText(labels.systemOnline, { exact: false }),
  };
}

export function resetPasswordPage(page: Page): {
  heading: Locator;
  phone: Locator;
  requestOtpButton: Locator;
  otpDigits: Locator;
  verifyOtpButton: Locator;
  newPassword: Locator;
  confirmPassword: Locator;
  confirmButton: Locator;
  completeButton: Locator;
  backToLoginButton: Locator;
} {
  return {
    heading: page.getByRole('heading', {
      name: labels.resetHeading,
      exact: true,
    }),
    phone: page.getByRole('textbox', { name: labels.phone, exact: true }),
    requestOtpButton: page.getByRole('button', {
      name: labels.requestOtp,
      exact: true,
    }),
    otpDigits: page.getByRole('textbox', { name: /^Mã OTP [1-6]$/ }),
    verifyOtpButton: page.getByRole('button', {
      name: labels.verifyOtp,
      exact: true,
    }),
    newPassword: page.getByLabel(labels.newPassword, { exact: true }),
    confirmPassword: page.getByLabel(labels.confirmPassword, { exact: true }),
    confirmButton: page.getByRole('button', {
      name: labels.confirmReset,
      exact: true,
    }),
    completeButton: page.getByRole('button', {
      name: labels.completeReset,
      exact: true,
    }),
    backToLoginButton: page.getByRole('button', {
      name: labels.backToLogin,
      exact: true,
    }),
  };
}

export function homePage(page: Page): {
  screen: Locator;
  profileButton: Locator;
  logoutButton: Locator;
  studentProfile: (
    fullName: string,
    className: string,
    gradeLevel: number,
    studentCode: string,
  ) => Locator;
  summaryCard: (label: string, value: number) => Locator;
  announcement: (title: string, body: string) => Locator;
  quickAction: (label: string) => Locator;
} {
  return {
    screen: page.getByRole('group', {
      name: labels.homeScreen,
      exact: true,
    }),
    profileButton: page.getByRole('button', {
      name: 'Mở trang cá nhân',
      exact: true,
    }),
    logoutButton: page.getByRole('button', {
      name: labels.logout,
      exact: true,
    }),
    studentProfile: (fullName, className, gradeLevel, studentCode) =>
      page.getByLabel(
        `Hồ sơ học sinh: ${fullName}, ${className}, khối ${gradeLevel}, mã học sinh ${studentCode}`,
        { exact: true },
      ),
    summaryCard: (label, value) =>
      page.getByText(`${label}: ${value}`, { exact: false }),
    announcement: (title, body) =>
      page.getByText(`Thông báo: ${title}. ${body}`, { exact: true }),
    quickAction: (label) => page.getByRole('tab', { name: label, exact: true }),
  };
}

export function timetablePage(page: Page): {
  screen: Locator;
  previousWeekButton: Locator;
  nextWeekButton: Locator;
  weekRange: (weekStart: string, weekEnd: string) => Locator;
  dayButton: (date: string) => Locator;
  selectedDay: (date: string) => Locator;
  lesson: (lesson: TimetableLesson) => Locator;
  exceptionLesson: (lesson: TimetableLessonOccurrence) => Locator;
} {
  return {
    screen: page.getByRole('group', {
      name: 'Lịch học',
      exact: true,
    }),
    previousWeekButton: page.getByRole('button', {
      name: 'Tuần trước',
      exact: true,
    }),
    nextWeekButton: page.getByRole('button', {
      name: 'Tuần sau',
      exact: true,
    }),
    weekRange: (weekStart, weekEnd) =>
      page.getByLabel(`Tuần ${weekStart} đến ${weekEnd}`, { exact: true }),
    dayButton: (date) =>
      page.getByRole('button', {
        name: `Chọn ngày ${date}`,
        exact: true,
      }),
    selectedDay: (date) =>
      page.getByLabel(`Lịch ngày ${date}`, { exact: true }),
    lesson: (lesson) =>
      // Flutter Web exposes non-interactive card semantics as text nodes.
      page.getByText(
        new RegExp(
          `Tiết ${lesson.periodNumber}.*${escapeForRegex(lesson.subject.name)}.*${lesson.startTime}.*${lesson.endTime}`,
        ),
      ),
    exceptionLesson: (lesson) =>
      page.getByText(
        new RegExp(
          `Tiết ${lesson.periodNumber}.*${escapeForRegex(lesson.subject.name)}.*${escapeForRegex(lesson.note ?? '')}`,
        ),
      ),
  };
}

export function gradesPage(page: Page): {
  screen: Locator;
  termControl: (term: GradeAcademicTerm) => Locator;
  termOption: (term: GradeAcademicTerm) => Locator;
  subjectSummary: (subject: GradeSubject) => Locator;
  subjectDetailsButton: (subject: GradeSubject) => Locator;
  detailScreen: (subject: GradeSubject) => Locator;
  assessmentSection: (
    kind: 'REGULAR' | 'MIDTERM' | 'FINAL',
  ) => Locator;
  assessment: (assessment: GradeAssessment) => Locator;
} {
  return {
    screen: page.getByRole('group', {
      name: labels.gradesScreen,
      exact: true,
    }),
    termControl: (term) =>
      page.getByRole('button', {
        name: `Chọn học kỳ ${term.name}, ${term.academicYear}`,
        exact: true,
      }),
    termOption: (term) =>
      page.getByRole('button', {
        name: `Chọn ${term.name}, ${term.academicYear}`,
        exact: true,
      }),
    subjectSummary: (subject) =>
      page.getByRole('group', {
        name: new RegExp(`^Môn ${escapeForRegex(subject.name)}\\.`),
      }),
    subjectDetailsButton: (subject) =>
      page.getByRole('button', {
        name: `Xem chi tiết ${subject.name}`,
        exact: true,
      }),
    detailScreen: (subject) =>
      page.getByRole('group', {
        name: `Chi tiết điểm ${subject.name}`,
        exact: true,
      }),
    assessmentSection: (kind) =>
      page.getByRole('heading', {
        name: new RegExp(
          `^${escapeForRegex(assessmentSectionLabels[kind])}(?:\\s|$)`,
        ),
        level: 2,
      }),
    assessment: (assessment) =>
      // Flutter Web combines non-interactive assessment-card semantics into text.
      page.getByText(
        new RegExp(`${escapeForRegex(assessment.displayLabel)}\\.`),
      ),
  };
}

export function eventsPage(page: Page): {
  eventsTab: Locator;
  screen: Locator;
  filterButton: Locator;
  includePastSwitch: Locator;
  filterOption: (category: EventCategory | 'ALL') => Locator;
  eventDetailsButton: (event: SchoolEvent) => Locator;
  detailScreen: (event: SchoolEvent) => Locator;
  registerButton: Locator;
  cancelButton: Locator;
} {
  return {
    eventsTab: page.getByRole('tab', {
      name: labels.eventsScreen,
      exact: true,
    }),
    screen: page.getByRole('group', {
      name: labels.eventsScreen,
      exact: true,
    }),
    filterButton: page.getByRole('button', {
      name: labels.eventsFilter,
      exact: true,
    }),
    includePastSwitch: page.getByRole('switch', {
      name: labels.includePastEvents,
      exact: true,
    }),
    filterOption: (category) =>
      page.getByRole('button', {
        name: eventFilterLabels[category],
        exact: true,
      }),
    eventDetailsButton: (event) =>
      page.getByRole('button', {
        name: `Xem sự kiện ${event.title}`,
        exact: true,
      }),
    detailScreen: (event) =>
      page.getByRole('group', {
        name: `Chi tiết sự kiện ${event.title}`,
        exact: true,
      }),
    registerButton: page.getByRole('button', {
      name: labels.registerEvent,
      exact: true,
    }),
    cancelButton: page.getByRole('button', {
      name: labels.cancelEvent,
      exact: true,
    }),
  };
}

export function formsPage(page: Page): {
  screen: Locator;
  createButton: Locator;
  filterButton: Locator;
  formCard: (form: StudentFormSummary) => Locator;
  detailsButton: (form: StudentFormSummary) => Locator;
  detailScreen: (form: StudentFormDetails | StudentFormSummary) => Locator;
  formTypeButton: Locator;
  formTypeOption: (type: StudentFormType) => Locator;
  reason: Locator;
  submitButton: Locator;
  cancelButton: Locator;
  confirmCancelButton: Locator;
  statusText: (label: string) => Locator;
} {
  return {
    screen: page.getByRole('group', {
      name: labels.formsScreen,
      exact: true,
    }),
    createButton: page.getByRole('button', {
      name: labels.createForm,
      exact: true,
    }),
    filterButton: page.getByRole('button', {
      name: 'Lọc trạng thái đơn',
      exact: true,
    }),
    formCard: (form) =>
      page.getByRole('group', {
        name: `Đơn ${studentFormTypeLabels[form.type]}. Trạng thái ${studentFormStatusLabels[form.status]}.`,
        exact: true,
      }),
    detailsButton: (form) =>
      page.getByRole('button', {
        name: `Xem đơn ${form.id}`,
        exact: true,
      }),
    detailScreen: (form) =>
      page.getByRole('group', {
        name: `Chi tiết đơn ${form.id}`,
        exact: true,
      }),
    formTypeButton: page.getByRole('button', {
      name: labels.formType,
      exact: true,
    }),
    formTypeOption: (type) =>
      page.getByRole('button', {
        name: `Chọn loại đơn ${studentFormTypeLabels[type]}`,
        exact: true,
      }),
    reason: page.getByRole('textbox', {
      name: /^Lý do(?:\s|$)/,
    }),
    submitButton: page.getByRole('button', {
      name: labels.submitForm,
      exact: true,
    }),
    cancelButton: page.getByRole('button', {
      name: labels.cancelForm,
      exact: true,
    }),
    confirmCancelButton: page.getByRole('button', {
      name: labels.confirmCancelForm,
      exact: true,
    }),
    statusText: (label) => page.getByText(`Trạng thái: ${label}`, { exact: true }),
  };
}

export function clubsPage(page: Page): {
  screen: Locator;
  filterButton: Locator;
  filterOption: (category: ClubCategory) => Locator;
  detailsButton: (club: SchoolClub) => Locator;
  detailScreen: (club: SchoolClub) => Locator;
  applyButton: Locator;
  withdrawButton: Locator;
  statusText: (label: string) => Locator;
} {
  const categoryLabels: Record<ClubCategory, string> = {
    ACADEMIC: 'Học thuật', SPORTS: 'Thể thao', ARTS: 'Nghệ thuật',
    SKILLS: 'Kỹ năng', COMMUNITY: 'Cộng đồng', MEDIA: 'Truyền thông',
  };
  return {
    screen: page.getByRole('group', { name: 'Câu lạc bộ', exact: true }),
    filterButton: page.getByRole('button', { name: 'Lọc câu lạc bộ', exact: true }),
    filterOption: (category) => page.getByRole('button', {
      name: `Lọc CLB: ${categoryLabels[category]}`, exact: true,
    }),
    detailsButton: (club) => page.getByRole('button', { name: `Xem CLB ${club.id}`, exact: true }),
    detailScreen: (club) => page.getByRole('group', { name: `Chi tiết CLB ${club.id}`, exact: true }),
    applyButton: page.getByRole('button', { name: 'Đăng ký CLB', exact: true }),
    withdrawButton: page.getByRole('button', { name: 'Rút đơn CLB', exact: true }),
    statusText: (label) => page.getByText(`Trạng thái: ${label}`, { exact: true }),
  };
}

export function assistantPage(page: Page): {
  screen: Locator;
  question: Locator;
  sendButton: Locator;
  answer: (text: string | RegExp) => Locator;
} {
  return {
    screen: page.getByRole('group', { name: 'Trợ lý học sinh', exact: true }),
    question: page.getByRole('textbox', { name: /^Câu hỏi(?:\s|$)/ }),
    sendButton: page.getByRole('button', { name: 'Gửi câu hỏi', exact: true }),
    answer: (text) => page.getByText(text),
  };
}

const assessmentSectionLabels = {
  REGULAR: 'Đánh giá thường xuyên',
  MIDTERM: 'Giữa kỳ',
  FINAL: 'Cuối kỳ',
} as const;

const eventFilterLabels: Record<EventCategory | 'ALL', string> = {
  ALL: 'Lọc: Tất cả',
  ACADEMIC: 'Lọc: Học tập',
  CULTURAL: 'Lọc: Văn hóa',
  SPORTS: 'Lọc: Thể thao',
  CLUB: 'Lọc: Câu lạc bộ',
  CAREER: 'Lọc: Hướng nghiệp',
};

const studentFormTypeLabels: Record<StudentFormType, string> = {
  LEAVE_OF_ABSENCE: 'Đơn xin nghỉ học',
  STUDENT_CONFIRMATION: 'Giấy xác nhận học sinh',
  TRANSCRIPT_REQUEST: 'Yêu cầu bảng điểm',
  STUDENT_CARD_REISSUE: 'Cấp lại thẻ học sinh',
};

const studentFormStatusLabels = {
  SUBMITTED: 'Đã gửi',
  IN_REVIEW: 'Đang xử lý',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
} as const;

function escapeForRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
