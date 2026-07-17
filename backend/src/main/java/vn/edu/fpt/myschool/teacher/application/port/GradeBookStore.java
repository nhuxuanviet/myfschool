package vn.edu.fpt.myschool.teacher.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.teacher.domain.GradeBookView;

public interface GradeBookStore {

    Optional<GradeBookView.Book> findBook(UUID bookId);

    Optional<GradeBookView.Book> findBookBySlot(UUID classId, UUID subjectId, UUID academicTermId);

    UUID createBook(UUID classId, UUID subjectId, UUID academicTermId, Instant now);

    List<GradeBookView.Column> findColumns(UUID bookId);

    UUID createColumn(
            UUID bookId,
            String assessmentKind,
            String assessmentForm,
            String displayLabel,
            Integer durationMinutes,
            int displayOrder,
            Instant now);

    int nextColumnOrder(UUID bookId);

    /**
     * Creates one empty cell per student in the class.
     *
     * <p>Done as part of creating the column so the class's table is complete the moment the
     * column exists, rather than filling in as marks arrive and leaving gaps that look identical
     * to "this student has no mark".
     *
     * @return how many cells were created
     */
    int createPendingMarks(UUID bookId, UUID columnId, Instant now);

    List<GradeBookView.Mark> findMarks(UUID bookId);

    Optional<UUID> findBookIdByAssessment(UUID assessmentId);

    boolean recordMark(
            UUID assessmentId, BigDecimal score, String outcome, String status, Instant now);

    boolean publish(UUID bookId, UUID publishedBy, long expectedVersion, Instant now);

    boolean unpublish(UUID bookId, long expectedVersion, Instant now);
}
