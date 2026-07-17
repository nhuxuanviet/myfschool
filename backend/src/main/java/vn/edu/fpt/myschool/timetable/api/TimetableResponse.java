package vn.edu.fpt.myschool.timetable.api;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;
import vn.edu.fpt.myschool.timetable.domain.Timetable;
import vn.edu.fpt.myschool.timetable.domain.TimetableDay;
import vn.edu.fpt.myschool.timetable.domain.TimetableLesson;
import vn.edu.fpt.myschool.timetable.domain.TimetableSubject;

public record TimetableResponse(
        String timeZone,
        LocalDate weekStart,
        LocalDate weekEnd,
        List<AcademicTermResponse> academicTerms,
        List<Day> days) {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /** Public so the guardian view reports the same shape rather than a parallel one. */
    public static TimetableResponse from(Timetable timetable) {
        return new TimetableResponse(
                SchoolTimeZone.ZONE.getId(),
                timetable.weekStart(),
                timetable.weekEnd(),
                timetable.academicTerms().stream().map(AcademicTermResponse::from).toList(),
                timetable.days().stream().map(Day::from).toList());
    }

    /** Matches the JSON shape exposed by the home dashboard's academicTerm. */
    public record AcademicTermResponse(
            String academicYear,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn) {

        private static AcademicTermResponse from(AcademicTerm academicTerm) {
            return new AcademicTermResponse(
                    academicTerm.academicYear(),
                    academicTerm.code(),
                    academicTerm.name(),
                    academicTerm.startsOn(),
                    academicTerm.endsOn());
        }
    }

    public record Day(LocalDate date, String dayOfWeek, List<Lesson> lessons) {

        private static Day from(TimetableDay day) {
            return new Day(
                    day.date(),
                    day.dayOfWeek().name(),
                    day.lessons().stream().map(Lesson::from).toList());
        }
    }

    public record Lesson(
            String session,
            int periodNumber,
            String startTime,
            String endTime,
            Subject subject,
            String teacherName,
            String room,
            String status,
            String note) {

        private static Lesson from(TimetableLesson lesson) {
            return new Lesson(
                    lesson.session().name(),
                    lesson.periodNumber(),
                    TIME_FORMATTER.format(lesson.startTime()),
                    TIME_FORMATTER.format(lesson.endTime()),
                    Subject.from(lesson.subject()),
                    lesson.teacherName(),
                    lesson.room(),
                    lesson.status().name(),
                    lesson.note());
        }
    }

    public record Subject(String code, String name) {

        private static Subject from(TimetableSubject subject) {
            return new Subject(subject.code(), subject.name());
        }
    }
}
