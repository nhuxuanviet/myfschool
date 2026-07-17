package vn.edu.fpt.myschool.parent.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.grades.application.GradesService;
import vn.edu.fpt.myschool.grades.domain.SemesterGrades;
import vn.edu.fpt.myschool.parent.application.port.ParentStore;
import vn.edu.fpt.myschool.parent.domain.ParentChild;
import vn.edu.fpt.myschool.timetable.application.TimetableService;
import vn.edu.fpt.myschool.timetable.domain.Timetable;

/**
 * A guardian's view of their own children.
 *
 * <p>Nothing here reads a student until {@link #requireLinkedChild} has confirmed the link. The
 * marks themselves come from the same service the student app uses, so a guardian sees exactly
 * what has been published and nothing more, by construction rather than by a second rule.
 */
@Service
public class ParentService {

    private final ParentStore store;
    private final GradesService gradesService;
    private final TimetableService timetableService;

    public ParentService(
            ParentStore store, GradesService gradesService, TimetableService timetableService) {
        this.store = store;
        this.gradesService = gradesService;
        this.timetableService = timetableService;
    }

    @Transactional(readOnly = true)
    public List<ParentChild> getChildren(UUID userId) {
        return store.findChildren(requireParentId(userId));
    }

    @Transactional(readOnly = true)
    public SemesterGrades getChildGrades(UUID userId, UUID studentId, UUID academicTermId) {
        requireLinkedChild(userId, studentId);
        return gradesService.getSemesterGradesOf(studentId, academicTermId);
    }

    @Transactional(readOnly = true)
    public Timetable getChildTimetable(
            UUID userId, UUID studentId, java.time.LocalDate weekStart) {
        // Finding the child among the guardian's own children both proves the link and yields
        // the class, so the class code can never come from the caller.
        ParentChild child = linkedChild(userId, studentId);
        return timetableService.getTimetableOfClass(child.className(), weekStart);
    }

    private ParentChild linkedChild(UUID userId, UUID studentId) {
        return store.findChildren(requireParentId(userId)).stream()
                .filter(child -> child.studentId().equals(studentId))
                .findFirst()
                .orElseThrow(ParentException::notLinked);
    }

    private void requireLinkedChild(UUID userId, UUID studentId) {
        if (!store.isLinkedToStudent(requireParentId(userId), studentId)) {
            throw ParentException.notLinked();
        }
    }

    private UUID requireParentId(UUID userId) {
        return store.findParentIdByUserId(userId).orElseThrow(ParentException::noParentProfile);
    }
}
