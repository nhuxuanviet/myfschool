package vn.edu.fpt.myschool.parent.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.grades.application.GradesService;
import vn.edu.fpt.myschool.grades.domain.SemesterGrades;
import vn.edu.fpt.myschool.parent.application.port.ParentStore;
import vn.edu.fpt.myschool.parent.domain.ParentChild;

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

    public ParentService(ParentStore store, GradesService gradesService) {
        this.store = store;
        this.gradesService = gradesService;
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

    private void requireLinkedChild(UUID userId, UUID studentId) {
        if (!store.isLinkedToStudent(requireParentId(userId), studentId)) {
            throw ParentException.notLinked();
        }
    }

    private UUID requireParentId(UUID userId) {
        return store.findParentIdByUserId(userId).orElseThrow(ParentException::noParentProfile);
    }
}
