package vn.edu.fpt.myschool.teacher.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A grade book as the responsible teacher sees it. */
public final class GradeBookView {

    private GradeBookView() {
    }

    /**
     * @param publishedAt when the subject teacher made the marks visible to students and guardians
     * @param lockedAt when the administration stopped direct editing. Independent of publication:
     *     a book can be published and still open, or locked and never published.
     */
    public record Book(
            UUID id,
            UUID classId,
            String classCode,
            UUID subjectId,
            String subjectCode,
            String subjectName,
            UUID academicTermId,
            String academicTermCode,
            Instant publishedAt,
            Instant lockedAt,
            long version) {

        public Book {
            Objects.requireNonNull(id, "id must not be null");
        }

        public boolean isPublished() {
            return publishedAt != null;
        }

        public boolean isLocked() {
            return lockedAt != null;
        }
    }

    public record Column(
            UUID id,
            String assessmentKind,
            String assessmentForm,
            String displayLabel,
            Integer durationMinutes,
            int displayOrder) {

        public Column {
            Objects.requireNonNull(id, "id must not be null");
        }
    }

    /**
     * One cell.
     *
     * @param status PENDING while the teacher has created the column but not entered this
     *     student's mark yet.
     */
    public record Mark(
            UUID assessmentId,
            UUID columnId,
            UUID studentId,
            String status,
            BigDecimal score,
            String outcome) {

        public Mark {
            Objects.requireNonNull(assessmentId, "assessmentId must not be null");
            Objects.requireNonNull(columnId, "columnId must not be null");
            Objects.requireNonNull(studentId, "studentId must not be null");
        }
    }

    public record Sheet(
            Book book,
            List<Column> columns,
            List<TeacherWorkload.ClassStudent> students,
            List<Mark> marks) {

        public Sheet {
            Objects.requireNonNull(book, "book must not be null");
            columns = List.copyOf(Objects.requireNonNull(columns, "columns must not be null"));
            students = List.copyOf(Objects.requireNonNull(students, "students must not be null"));
            marks = List.copyOf(Objects.requireNonNull(marks, "marks must not be null"));
        }
    }
}
