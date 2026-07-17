package vn.edu.fpt.myschool.teacher.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.teacher.application.port.GradeBookStore;
import vn.edu.fpt.myschool.teacher.application.port.TeacherStore;
import vn.edu.fpt.myschool.teacher.domain.GradeBookView;

/**
 * The grade book, from the responsible teacher's side.
 *
 * <p>Every entry point resolves the teacher from the authenticated user and checks the assignment
 * before touching the book. A book id from the client decides nothing on its own.
 */
@Service
public class GradeBookService {

    private final GradeBookStore store;
    private final TeacherStore teacherStore;
    private final TeacherService teacherService;
    private final Clock clock;

    public GradeBookService(
            GradeBookStore store,
            TeacherStore teacherStore,
            TeacherService teacherService,
            Clock clock) {
        this.store = store;
        this.teacherStore = teacherStore;
        this.teacherService = teacherService;
        this.clock = clock;
    }

    /** Opens the book for a class-subject-term, creating it the first time the teacher asks. */
    @Transactional
    public GradeBookView.Book openBook(
            UUID userId, UUID classId, UUID subjectId, UUID academicTermId) {
        UUID teacherId = requireAssignment(userId, classId, subjectId, academicTermId);
        assert teacherId != null;
        return store.findBookBySlot(classId, subjectId, academicTermId)
                .orElseGet(() -> {
                    UUID id = store.createBook(classId, subjectId, academicTermId, clock.instant());
                    return store.findBook(id).orElseThrow(GradeBookException::notFound);
                });
    }

    @Transactional(readOnly = true)
    public GradeBookView.Sheet getSheet(UUID userId, UUID bookId) {
        GradeBookView.Book book = requireOwnBook(userId, bookId);
        return new GradeBookView.Sheet(
                book,
                store.findColumns(bookId),
                teacherStore.findClassStudents(book.classId()),
                store.findMarks(bookId));
    }

    /**
     * Adds a column and opens an empty cell for every enrolled student.
     *
     * <p>The cells are created with the column, not as marks arrive: a column that exists for the
     * class but only for some students is exactly the ragged table this design removes.
     */
    @Transactional
    public UUID addColumn(
            UUID userId,
            UUID bookId,
            String assessmentKind,
            String assessmentForm,
            String displayLabel,
            Integer durationMinutes) {
        GradeBookView.Book book = requireEditableBook(userId, bookId);
        UUID columnId = store.createColumn(
                book.id(), assessmentKind, assessmentForm, displayLabel.strip(),
                durationMinutes, store.nextColumnOrder(book.id()), clock.instant());
        store.createPendingMarks(book.id(), columnId, clock.instant());
        return columnId;
    }

    @Transactional
    public void recordMark(UUID userId, UUID assessmentId, BigDecimal score, String outcome) {
        UUID bookId = store.findBookIdByAssessment(assessmentId)
                .orElseThrow(GradeBookException::notFound);
        requireEditableBook(userId, bookId);
        if ((score == null) == (outcome == null)) {
            throw GradeBookException.wrongResultForMode();
        }
        if (!store.recordMark(assessmentId, score, outcome, "RECORDED", clock.instant())) {
            throw GradeBookException.notFound();
        }
    }

    /** Publishing is the teacher's call: it says these marks are ready to be seen. */
    @Transactional
    public void publish(UUID userId, UUID bookId, long expectedVersion) {
        GradeBookView.Book book = requireOwnBook(userId, bookId);
        if (!store.publish(book.id(), userId, expectedVersion, clock.instant())) {
            throw GradeBookException.stale();
        }
    }

    @Transactional
    public void unpublish(UUID userId, UUID bookId, long expectedVersion) {
        GradeBookView.Book book = requireOwnBook(userId, bookId);
        if (!store.unpublish(book.id(), expectedVersion, clock.instant())) {
            throw GradeBookException.stale();
        }
    }

    /** Publication does not restrict editing; only locking does (spec §5.5). */
    private GradeBookView.Book requireEditableBook(UUID userId, UUID bookId) {
        GradeBookView.Book book = requireOwnBook(userId, bookId);
        if (book.isLocked()) {
            throw GradeBookException.locked();
        }
        return book;
    }

    private GradeBookView.Book requireOwnBook(UUID userId, UUID bookId) {
        GradeBookView.Book book = store.findBook(bookId).orElseThrow(GradeBookException::notFound);
        requireAssignment(userId, book.classId(), book.subjectId(), book.academicTermId());
        return book;
    }

    private UUID requireAssignment(
            UUID userId, UUID classId, UUID subjectId, UUID academicTermId) {
        UUID teacherId = teacherService.requireTeacherId(userId);
        if (!teacherStore.isAssignedToSubject(teacherId, classId, subjectId, academicTermId)) {
            throw TeacherException.notAssignedToSubject();
        }
        return teacherId;
    }
}
