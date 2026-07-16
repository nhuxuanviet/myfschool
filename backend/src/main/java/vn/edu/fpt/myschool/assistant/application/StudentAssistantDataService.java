package vn.edu.fpt.myschool.assistant.application;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import vn.edu.fpt.myschool.clubs.application.ClubsService;
import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.clubs.domain.ClubDetails;
import vn.edu.fpt.myschool.clubs.domain.ClubMembershipStatus;
import vn.edu.fpt.myschool.events.application.EventsService;
import vn.edu.fpt.myschool.events.domain.EventDetails;
import vn.edu.fpt.myschool.events.domain.EventRegistrationStatus;
import vn.edu.fpt.myschool.forms.application.StudentFormsService;
import vn.edu.fpt.myschool.forms.domain.StudentFormStatus;
import vn.edu.fpt.myschool.forms.domain.StudentFormSummary;
import vn.edu.fpt.myschool.forms.domain.StudentFormType;
import vn.edu.fpt.myschool.grades.application.GradesService;
import vn.edu.fpt.myschool.grades.domain.SemesterGradeSubject;
import vn.edu.fpt.myschool.grades.domain.SemesterGrades;
import vn.edu.fpt.myschool.grades.domain.TermResult;
import vn.edu.fpt.myschool.home.application.HomeDashboardService;
import vn.edu.fpt.myschool.home.domain.HomeAnnouncement;
import vn.edu.fpt.myschool.home.domain.HomeDashboard;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;
import vn.edu.fpt.myschool.timetable.application.TimetableService;
import vn.edu.fpt.myschool.timetable.domain.Timetable;
import vn.edu.fpt.myschool.timetable.domain.TimetableDay;
import vn.edu.fpt.myschool.timetable.domain.TimetableLesson;

@Service
class StudentAssistantDataService implements StudentAssistantData {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter INSTANT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(SchoolTimeZone.ZONE);
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final TimetableService timetableService;
    private final GradesService gradesService;
    private final EventsService eventsService;
    private final StudentFormsService formsService;
    private final ClubsService clubsService;
    private final HomeDashboardService homeService;
    private final StudentProfileStore studentProfileStore;
    private final Clock clock;

    StudentAssistantDataService(
            TimetableService timetableService,
            GradesService gradesService,
            EventsService eventsService,
            StudentFormsService formsService,
            ClubsService clubsService,
            HomeDashboardService homeService,
            StudentProfileStore studentProfileStore,
            Clock clock) {
        this.timetableService = timetableService;
        this.gradesService = gradesService;
        this.eventsService = eventsService;
        this.formsService = formsService;
        this.clubsService = clubsService;
        this.homeService = homeService;
        this.studentProfileStore = studentProfileStore;
        this.clock = clock;
    }

    @Override
    public String profile(String authenticatedUserId) {
        StudentProfile profile = studentProfileStore.findByUserId(UUID.fromString(authenticatedUserId))
                .orElseThrow(() -> new IllegalStateException("Authenticated student profile was not found."));
        return "Hồ sơ học sinh: %s, mã học sinh %s, lớp %s, khối %d."
                .formatted(profile.fullName(), profile.studentCode(), profile.className(), profile.gradeLevel());
    }

    @Override
    public String timetable(String authenticatedUserId) {
        Timetable timetable = timetableService.getTimetable(authenticatedUserId, null);
        String lessons = timetable.days().stream()
                .filter(day -> !day.lessons().isEmpty())
                .map(this::formatDay)
                .collect(Collectors.joining("\n"));
        if (lessons.isBlank()) {
            lessons = "Không có tiết học trong tuần này.";
        }
        return "Thời khóa biểu tuần %s - %s:\n%s".formatted(
                DATE.format(timetable.weekStart()), DATE.format(timetable.weekEnd()), lessons);
    }

    @Override
    public String timetableForDay(String authenticatedUserId, int dayOffset) {
        if (dayOffset < 0 || dayOffset > 7) {
            throw new IllegalArgumentException("Day offset must be between 0 and 7.");
        }
        LocalDate date = LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE).plusDays(dayOffset);
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        Timetable timetable = timetableService.getTimetable(authenticatedUserId, weekStart);
        TimetableDay day = timetable.days().stream()
                .filter(candidate -> candidate.date().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Requested timetable day was not initialized."));
        String relativeLabel = switch (dayOffset) {
            case 0 -> "hôm nay";
            case 1 -> "ngày mai";
            default -> "ngày " + DATE.format(date);
        };
        String dayName = capitalize(
                day.dayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, VIETNAMESE));
        if (day.lessons().isEmpty()) {
            return "Em không có tiết học %s, %s %s.".formatted(
                    relativeLabel, dayName, DATE.format(date));
        }
        return "Lịch học %s, %s %s:\n%s".formatted(
                relativeLabel,
                dayName,
                DATE.format(date),
                day.lessons().stream().map(this::formatLesson).collect(Collectors.joining("\n")));
    }

    @Override
    public String grades(String authenticatedUserId) {
        SemesterGrades grades = gradesService.getSemesterGrades(authenticatedUserId, null);
        String subjects = grades.subjects().stream()
                .map(this::formatSubject)
                .collect(Collectors.joining("\n"));
        return "Điểm %s, năm học %s:\n%s".formatted(
                grades.selectedTerm().name(), grades.selectedTerm().academicYear(), subjects);
    }

    @Override
    public SubjectGradeFact gradeForSubject(String authenticatedUserId, String subjectName) {
        SemesterGrades grades = gradesService.getSemesterGrades(authenticatedUserId, null);
        String normalizedQuery = normalizeSearchText(subjectName);
        return grades.subjects().stream()
                .filter(subject -> matchesSubject(normalizedQuery, subject))
                .findFirst()
                .map(subject -> subjectGradeFact(grades, subject))
                .orElseGet(() -> new SubjectGradeFact(
                        grades.selectedTerm().name(),
                        grades.selectedTerm().academicYear(),
                        subjectName.trim(),
                        false,
                        false,
                        null,
                        null));
    }

    @Override
    public String events(String authenticatedUserId) {
        List<EventDetails> events = eventsService.getEvents(authenticatedUserId, null, false);
        if (events.isEmpty()) {
            return "Hiện không có sự kiện sắp tới dành cho em.";
        }
        return "Sự kiện sắp tới:\n" + events.stream()
                .map(this::formatEvent)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String forms(String authenticatedUserId) {
        List<StudentFormSummary> forms = formsService.getForms(authenticatedUserId, null);
        if (forms.isEmpty()) {
            return "Em chưa có đơn từ nào.";
        }
        return "Đơn từ của em:\n" + forms.stream()
                .map(form -> "- %s: %s (cập nhật %s)".formatted(
                        formTypeLabel(form.type()),
                        formStatusLabel(form.status()),
                        INSTANT.format(form.updatedAt())))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String clubs(String authenticatedUserId) {
        List<ClubDetails> clubs = clubsService.getClubs(authenticatedUserId, null);
        if (clubs.isEmpty()) {
            return "Hiện không có câu lạc bộ phù hợp với khối của em.";
        }
        return "Câu lạc bộ dành cho em:\n" + clubs.stream()
                .map(this::formatClub)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String announcements(String authenticatedUserId) {
        HomeDashboard dashboard = homeService.getDashboard(authenticatedUserId);
        if (dashboard.announcements().isEmpty()) {
            return "Hiện chưa có thông báo mới.";
        }
        return "Thông báo mới:\n" + dashboard.announcements().stream()
                .map(this::formatAnnouncement)
                .collect(Collectors.joining("\n"));
    }

    private String formatDay(TimetableDay day) {
        String dayName = day.dayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, VIETNAMESE);
        return "%s %s:\n%s".formatted(
                capitalize(dayName), DATE.format(day.date()), day.lessons().stream()
                        .map(this::formatLesson)
                        .collect(Collectors.joining("\n")));
    }

    private String formatLesson(TimetableLesson lesson) {
        return "  - Tiết %d (%s-%s): %s%s".formatted(
                lesson.periodNumber(),
                TIME.format(lesson.startTime()),
                TIME.format(lesson.endTime()),
                lesson.subject().name(),
                lesson.room() == null ? "" : " tại " + lesson.room());
    }

    private String formatSubject(SemesterGradeSubject subject) {
        String result = subject.termAverage() != null
                ? formatDecimal(subject.termAverage())
                : subject.termResult() != null ? termResultLabel(subject.termResult()) : "chưa đủ dữ liệu";
        return "- %s: %s".formatted(subject.name(), result);
    }

    private SubjectGradeFact subjectGradeFact(SemesterGrades grades, SemesterGradeSubject subject) {
        String qualitativeResult = subject.termResult() == null || subject.termResult() == TermResult.PENDING
                ? null
                : termResultLabel(subject.termResult());
        boolean resultAvailable = subject.termAverage() != null || qualitativeResult != null;
        return new SubjectGradeFact(
                grades.selectedTerm().name(),
                grades.selectedTerm().academicYear(),
                subject.name(),
                true,
                resultAvailable,
                subject.termAverage(),
                qualitativeResult);
    }

    private String formatEvent(EventDetails details) {
        return "- %s, %s tại %s (đăng ký: %s)".formatted(
                details.event().title(),
                INSTANT.format(details.event().startsAt()),
                details.event().location(),
                eventRegistrationLabel(details.registrationStatus()));
    }

    private String formatClub(ClubDetails details) {
        return "- %s: %s, %s tại %s".formatted(
                details.club().name(),
                clubMembershipLabel(details.membershipStatus()),
                details.club().meetingSchedule(),
                details.club().location());
    }

    private String formatAnnouncement(HomeAnnouncement announcement) {
        return "- %s (%s): %s".formatted(
                announcement.title(), INSTANT.format(announcement.publishedAt()), announcement.body());
    }

    private static String formatDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String normalizeSearchText(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(VIETNAMESE)
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean matchesSubject(String normalizedQuery, SemesterGradeSubject subject) {
        if (normalizedQuery.contains(normalizeSearchText(subject.name()))) {
            return true;
        }
        return switch (subject.code()) {
            case "TOAN" -> containsWord(normalizedQuery, "toan");
            case "NGU_VAN" -> containsWord(normalizedQuery, "van");
            case "TIENG_ANH" -> containsWord(normalizedQuery, "anh");
            case "VAT_LI" -> containsWord(normalizedQuery, "ly") || containsWord(normalizedQuery, "li");
            case "HOA_HOC" -> containsWord(normalizedQuery, "hoa");
            case "SINH_HOC" -> containsWord(normalizedQuery, "sinh");
            case "LICH_SU" -> containsWord(normalizedQuery, "su");
            case "DIA_LI" -> containsWord(normalizedQuery, "dia");
            case "TIN_HOC" -> containsWord(normalizedQuery, "tin");
            case "GIAO_DUC_THE_CHAT" -> normalizedQuery.contains("the duc");
            default -> false;
        };
    }

    private static boolean containsWord(String value, String word) {
        return (" " + value + " ").contains(" " + word + " ");
    }

    private static String eventRegistrationLabel(EventRegistrationStatus status) {
        return switch (status) {
            case NOT_REGISTERED -> "Chưa đăng ký";
            case REGISTERED -> "Đã đăng ký";
            case CANCELLED -> "Đã hủy đăng ký";
        };
    }

    private static String clubMembershipLabel(ClubMembershipStatus status) {
        return switch (status) {
            case NOT_APPLIED -> "Chưa đăng ký";
            case PENDING -> "Đang chờ duyệt";
            case ACTIVE -> "Đang tham gia";
            case REJECTED -> "Bị từ chối";
            case WITHDRAWN -> "Đã rút đơn";
        };
    }

    private static String formTypeLabel(StudentFormType type) {
        return switch (type) {
            case LEAVE_OF_ABSENCE -> "Đơn xin nghỉ học";
            case STUDENT_CONFIRMATION -> "Đơn xác nhận học sinh";
            case TRANSCRIPT_REQUEST -> "Đơn xin bảng điểm";
            case STUDENT_CARD_REISSUE -> "Đơn cấp lại thẻ học sinh";
        };
    }

    private static String formStatusLabel(StudentFormStatus status) {
        return switch (status) {
            case SUBMITTED -> "Đã gửi";
            case IN_REVIEW -> "Đang xử lý";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }

    private static String termResultLabel(TermResult result) {
        return switch (result) {
            case ACHIEVED -> "Đạt";
            case NOT_ACHIEVED -> "Chưa đạt";
            case PENDING -> "Chưa có kết quả";
        };
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(VIETNAMESE) + value.substring(1);
    }
}
