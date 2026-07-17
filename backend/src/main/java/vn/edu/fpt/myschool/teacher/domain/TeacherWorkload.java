package vn.edu.fpt.myschool.teacher.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** What a teacher is responsible for, and nothing else. */
public final class TeacherWorkload {

    private TeacherWorkload() {
    }

    /**
     * A class-subject-term the teacher is assigned to.
     *
     * @param homeroom whether this teacher is also the homeroom teacher of the class. Homeroom
     *     duty is a separate relationship, so it is reported rather than inferred from teaching.
     */
    public record AssignedClass(
            UUID assignmentId,
            UUID classId,
            String classCode,
            int gradeLevel,
            UUID subjectId,
            String subjectCode,
            String subjectName,
            UUID academicTermId,
            String academicTermCode,
            int studentCount,
            boolean homeroom) {

        public AssignedClass {
            Objects.requireNonNull(assignmentId, "assignmentId must not be null");
            Objects.requireNonNull(classId, "classId must not be null");
            Objects.requireNonNull(subjectId, "subjectId must not be null");
            if (studentCount < 0) {
                throw new IllegalArgumentException("studentCount must not be negative");
            }
        }
    }

    /** One lesson the teacher is scheduled to teach. */
    public record ScheduledLesson(
            LocalDate lessonDate,
            String session,
            int periodNumber,
            LocalTime startTime,
            LocalTime endTime,
            String classCode,
            String subjectCode,
            String subjectName,
            String room,
            String status) {

        public ScheduledLesson {
            Objects.requireNonNull(lessonDate, "lessonDate must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }
    }

    public record TeachingWeek(LocalDate weekStart, List<ScheduledLesson> lessons) {

        public TeachingWeek {
            Objects.requireNonNull(weekStart, "weekStart must not be null");
            lessons = List.copyOf(Objects.requireNonNull(lessons, "lessons must not be null"));
        }
    }

    /** A student in a class the teacher is assigned to. */
    public record ClassStudent(
            UUID studentId,
            String studentCode,
            String fullName,
            int gradeLevel) {

        public ClassStudent {
            Objects.requireNonNull(studentId, "studentId must not be null");
        }
    }

    /** A class this teacher is the homeroom teacher of. */
    public record HomeroomClass(
            UUID classId,
            String classCode,
            int gradeLevel,
            UUID academicYearId,
            int studentCount) {

        public HomeroomClass {
            Objects.requireNonNull(classId, "classId must not be null");
        }
    }
}
