package vn.edu.fpt.myschool.timetable.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;
import vn.edu.fpt.myschool.timetable.application.port.TimetableStore;
import vn.edu.fpt.myschool.timetable.domain.SchoolSession;
import vn.edu.fpt.myschool.timetable.domain.Timetable;
import vn.edu.fpt.myschool.timetable.domain.TimetableDay;
import vn.edu.fpt.myschool.timetable.domain.TimetableLesson;
import vn.edu.fpt.myschool.timetable.domain.TimetableLessonOccurrence;
import vn.edu.fpt.myschool.timetable.domain.TimetableTerm;

@Service
public class TimetableService {

    private static final Comparator<TimetableLesson> LESSON_ORDER = Comparator
            .comparing(TimetableLesson::session)
            .thenComparing(TimetableLesson::startTime)
            .thenComparingInt(TimetableLesson::periodNumber);

    private static final Comparator<TimetableTerm> TERM_ORDER = Comparator
            .comparing((TimetableTerm term) -> term.academicTerm().startsOn())
            .thenComparing(term -> term.academicTerm().endsOn())
            .thenComparing(term -> term.academicTerm().code())
            .thenComparing(term -> term.academicTerm().academicYear())
            .thenComparing(TimetableTerm::id);

    private final StudentProfileStore studentProfileStore;
    private final TimetableStore timetableStore;
    private final Clock clock;

    public TimetableService(
            StudentProfileStore studentProfileStore,
            TimetableStore timetableStore,
            Clock clock) {
        this.studentProfileStore = studentProfileStore;
        this.timetableStore = timetableStore;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Timetable getTimetable(String authenticatedUserId, LocalDate requestedWeekStart) {
        UUID userId = parseAuthenticatedUserId(authenticatedUserId);
        StudentProfile student = studentProfileStore.findByUserId(userId)
                .orElseThrow(TimetableException::studentProfileNotFound);
        return getTimetableOfClass(student.className(), requestedWeekStart);
    }

    /**
     * The same week, for a class the caller has already been established as entitled to see.
     *
     * <p>Takes a class code because a guardian is not the student. Whether the caller may look at
     * this class is the caller's business to prove; the timetable itself is the same one either
     * way, so there is only one place it can drift.
     */
    @Transactional(readOnly = true)
    public Timetable getTimetableOfClass(String className, LocalDate requestedWeekStart) {
        LocalDate weekStart = resolveWeekStart(requestedWeekStart);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<TimetableDay> days = initializeWeek(weekStart);
        List<TimetableTerm> academicTerms = timetableStore
                .findAcademicTermsOverlapping(weekStart, weekEnd)
                .stream()
                .filter(term -> intersects(term.academicTerm(), weekStart, weekEnd))
                .sorted(TERM_ORDER)
                .toList();
        Map<LessonSlot, TimetableLessonOccurrence> effectiveOccurrences = new HashMap<>();
        for (TimetableTerm term : academicTerms) {
            for (TimetableLessonOccurrence occurrence : timetableStore.findLessons(
                    term.id(),
                    className,
                    weekStart,
                    weekEnd)) {
                // PostgreSQL prevents overlaps within an academic year. This map
                // is defense-in-depth for malformed or legacy store data and
                // resolves a collision using the later deterministic term order.
                effectiveOccurrences.put(LessonSlot.from(occurrence), occurrence);
            }
        }
        addLessons(days, effectiveOccurrences.values());
        return new Timetable(
                weekStart,
                weekEnd,
                academicTerms.stream().map(TimetableTerm::academicTerm).toList(),
                days);
    }

    private LocalDate resolveWeekStart(LocalDate requestedWeekStart) {
        if (requestedWeekStart != null) {
            if (requestedWeekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
                throw TimetableException.invalidWeekStart();
            }
            return requestedWeekStart;
        }
        LocalDate schoolDate = LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE);
        return schoolDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<TimetableDay> initializeWeek(LocalDate weekStart) {
        List<TimetableDay> days = new ArrayList<>(7);
        for (int index = 0; index < 7; index++) {
            LocalDate date = weekStart.plusDays(index);
            days.add(new TimetableDay(date, date.getDayOfWeek(), new ArrayList<>()));
        }
        return days;
    }

    private void addLessons(
            List<TimetableDay> days,
            Collection<TimetableLessonOccurrence> occurrences) {
        Map<LocalDate, List<TimetableLesson>> lessonsByDate = new HashMap<>();
        for (TimetableDay day : days) {
            lessonsByDate.put(day.date(), new ArrayList<>());
        }
        for (TimetableLessonOccurrence occurrence : occurrences) {
            List<TimetableLesson> lessons = lessonsByDate.get(occurrence.date());
            if (lessons != null) {
                lessons.add(occurrence.lesson());
            }
        }
        for (int index = 0; index < days.size(); index++) {
            TimetableDay day = days.get(index);
            List<TimetableLesson> lessons = lessonsByDate.get(day.date());
            lessons.sort(LESSON_ORDER);
            days.set(index, new TimetableDay(day.date(), day.dayOfWeek(), lessons));
        }
    }

    private UUID parseAuthenticatedUserId(String authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw TimetableException.invalidAuthenticatedSubject();
        }
        try {
            return UUID.fromString(authenticatedUserId);
        } catch (IllegalArgumentException exception) {
            throw TimetableException.invalidAuthenticatedSubject();
        }
    }

    private static boolean intersects(AcademicTerm academicTerm, LocalDate weekStart, LocalDate weekEnd) {
        return !academicTerm.endsOn().isBefore(weekStart)
                && !academicTerm.startsOn().isAfter(weekEnd);
    }

    private record LessonSlot(LocalDate date, SchoolSession session, int periodNumber) {

        private static LessonSlot from(TimetableLessonOccurrence occurrence) {
            return new LessonSlot(
                    occurrence.date(),
                    occurrence.lesson().session(),
                    occurrence.lesson().periodNumber());
        }
    }
}
