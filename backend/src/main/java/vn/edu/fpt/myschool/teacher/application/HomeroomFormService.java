package vn.edu.fpt.myschool.teacher.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.shared.error.ApiException;
import vn.edu.fpt.myschool.teacher.application.port.HomeroomFormStore;
import vn.edu.fpt.myschool.teacher.domain.HomeroomForm;

/**
 * The homeroom teacher's leave-request queue.
 *
 * <p>Approving is the homeroom relationship, nothing else: teaching a subject to the student, or
 * being some other class's homeroom teacher, grants nothing here (spec §7.2).
 */
@Service
public class HomeroomFormService {

    private final HomeroomFormStore store;
    private final TeacherService teacherService;
    private final Clock clock;

    public HomeroomFormService(
            HomeroomFormStore store, TeacherService teacherService, Clock clock) {
        this.store = store;
        this.teacherService = teacherService;
        this.clock = clock;
    }

    public static final class HomeroomFormException extends ApiException {
        private HomeroomFormException(HttpStatus status, String code, String message) {
            super(status, code, message);
        }
    }

    /**
     * 403 for a form that is not this homeroom teacher's, whether it exists or not.
     *
     * <p>Distinguishing the two would let a teacher probe for other classes' forms.
     */
    private static HomeroomFormException notMine() {
        return new HomeroomFormException(
                HttpStatus.FORBIDDEN,
                "NOT_HOMEROOM_TEACHER_OF_STUDENT",
                "You are not the homeroom teacher of this student");
    }

    @Transactional(readOnly = true)
    public List<HomeroomForm> getLeaveForms(UUID userId, String status) {
        return store.findLeaveFormsForHomeroom(teacherService.requireTeacherId(userId), status);
    }

    @Transactional
    public void approve(UUID userId, UUID formId, String note) {
        decide(userId, formId, "APPROVED", note);
    }

    @Transactional
    public void reject(UUID userId, UUID formId, String note) {
        decide(userId, formId, "REJECTED", note);
    }

    private void decide(UUID userId, UUID formId, String status, String note) {
        UUID teacherId = teacherService.requireTeacherId(userId);
        if (!store.isHomeroomTeacherOfFormStudent(teacherId, formId)) {
            throw notMine();
        }
        // The store only accepts a leave form, so an administrative form cannot be decided here
        // even by the right teacher.
        HomeroomForm form = store.findLeaveForm(formId).orElseThrow(HomeroomFormService::notMine);
        if (!form.isOpen()) {
            throw new HomeroomFormException(
                    HttpStatus.CONFLICT,
                    "FORM_ALREADY_DECIDED",
                    "This form has already been decided or withdrawn");
        }
        if (!store.updateStatus(formId, status, clock.instant())) {
            throw new HomeroomFormException(
                    HttpStatus.CONFLICT,
                    "FORM_ALREADY_DECIDED",
                    "This form has already been decided or withdrawn");
        }
        // The history row names the decider, which is the whole point of approval having a trail.
        store.appendHistory(formId, status, userId, note, clock.instant());
    }
}
