package vn.edu.fpt.myschool.timetable.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.timetable.application.port.TimetableStore;
import vn.edu.fpt.myschool.timetable.domain.SchoolSession;
import vn.edu.fpt.myschool.timetable.domain.Timetable;
import vn.edu.fpt.myschool.timetable.domain.TimetableLesson;
import vn.edu.fpt.myschool.timetable.domain.TimetableLessonOccurrence;
import vn.edu.fpt.myschool.timetable.domain.TimetableLessonStatus;
import vn.edu.fpt.myschool.timetable.domain.TimetableSubject;
import vn.edu.fpt.myschool.timetable.domain.TimetableTerm;

class TimetableServiceTest {

    @Test
    void defaultsTheWeekUsingHoChiMinhCalendarDateInsteadOfTheUtcClockZone() {
        UUID userId = UUID.randomUUID();
        Clock utcClockAtSundayEvening = Clock.fixed(
                Instant.parse("2026-07-12T18:30:00Z"), ZoneOffset.UTC);
        StudentProfileStore students = ignored -> Optional.of(new StudentProfile(
                UUID.randomUUID(),
                "SE1913001",
                "Nguyễn Minh Anh",
                10,
                "10A1"));
        TimetableStore timetableStore = new EmptyTimetableStore();
        TimetableService service = new TimetableService(students, timetableStore, utcClockAtSundayEvening);

        Timetable timetable = service.getTimetable(userId.toString(), null);

        // 18:30 Sunday UTC is 01:30 Monday in Asia/Ho_Chi_Minh.
        assertThat(timetable.weekStart()).isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(timetable.weekEnd()).isEqualTo(LocalDate.of(2026, 7, 19));
    }

    @Test
    void givesTheLaterStartingTermDeterministicPrecedenceForAnOverlappingLessonSlot() {
        UUID userId = UUID.randomUUID();
        UUID firstTermId = UUID.randomUUID();
        UUID laterTermId = UUID.randomUUID();
        LocalDate weekStart = LocalDate.of(2026, 7, 13);
        TimetableTerm firstTerm = new TimetableTerm(firstTermId, new AcademicTerm(
                "2026-2027",
                "HK1",
                "Học kỳ I",
                weekStart,
                weekStart.plusDays(4)));
        TimetableTerm laterTerm = new TimetableTerm(laterTermId, new AcademicTerm(
                "2026-2027",
                "HK2",
                "Học kỳ II",
                weekStart.plusDays(2),
                weekStart.plusDays(6)));
        StudentProfileStore students = ignored -> Optional.of(new StudentProfile(
                UUID.randomUUID(),
                "SE1913001",
                "Nguyễn Minh Anh",
                10,
                "10A1"));
        TimetableStore store = new TimetableStore() {
            @Override
            public List<TimetableTerm> findAcademicTermsOverlapping(
                    LocalDate requestedWeekStart,
                    LocalDate requestedWeekEnd) {
                // Deliberately unsorted: the service owns chronological ordering.
                return List.of(laterTerm, firstTerm);
            }

            @Override
            public List<TimetableLessonOccurrence> findLessons(
                    UUID academicTermId,
                    String className,
                    LocalDate requestedWeekStart,
                    LocalDate requestedWeekEnd) {
                TimetableSubject subject = academicTermId.equals(firstTermId)
                        ? new TimetableSubject("TERM_A", "Term A")
                        : new TimetableSubject("TERM_B", "Term B");
                return List.of(new TimetableLessonOccurrence(
                        weekStart.plusDays(2),
                        new TimetableLesson(
                                SchoolSession.MORNING,
                                1,
                                LocalTime.of(7, 0),
                                LocalTime.of(7, 45),
                                subject,
                                null,
                                null,
                                TimetableLessonStatus.SCHEDULED,
                                null)));
            }
        };
        TimetableService service = new TimetableService(
                students,
                store,
                Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneOffset.UTC));

        Timetable timetable = service.getTimetable(userId.toString(), weekStart);

        assertThat(timetable.academicTerms()).extracting(AcademicTerm::code)
                .containsExactly("HK1", "HK2");
        assertThat(timetable.days().get(DayOfWeek.WEDNESDAY.getValue() - 1).lessons())
                .extracting(lesson -> lesson.subject().code())
                .containsExactly("TERM_B");
    }

    private static final class EmptyTimetableStore implements TimetableStore {

        @Override
        public List<TimetableTerm> findAcademicTermsOverlapping(
                LocalDate weekStart,
                LocalDate weekEnd) {
            return List.of();
        }

        @Override
        public List<TimetableLessonOccurrence> findLessons(
                UUID academicTermId,
                String className,
                LocalDate weekStart,
                LocalDate weekEnd) {
            return List.of();
        }
    }
}
