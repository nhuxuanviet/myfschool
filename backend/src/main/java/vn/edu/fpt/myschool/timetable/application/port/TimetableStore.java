package vn.edu.fpt.myschool.timetable.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.timetable.domain.TimetableLessonOccurrence;
import vn.edu.fpt.myschool.timetable.domain.TimetableTerm;

/**
 * Read model for a student's class timetable. Queries are scoped by the
 * student profile resolved from the authenticated account, never by a client
 * supplied student identifier.
 */
public interface TimetableStore {

    List<TimetableTerm> findAcademicTermsOverlapping(LocalDate weekStart, LocalDate weekEnd);

    List<TimetableLessonOccurrence> findLessons(
            UUID academicTermId,
            String className,
            LocalDate weekStart,
            LocalDate weekEnd);
}
