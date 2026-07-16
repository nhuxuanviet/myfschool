package vn.edu.fpt.myschool.forms.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.forms.application.port.StudentFormsStore;
import vn.edu.fpt.myschool.forms.domain.StudentFormDetails;
import vn.edu.fpt.myschool.forms.domain.StudentFormStatus;
import vn.edu.fpt.myschool.forms.domain.StudentFormSummary;
import vn.edu.fpt.myschool.forms.domain.StudentFormType;

@Service
public class StudentFormsService {

    private final StudentProfileStore studentProfileStore;
    private final StudentFormsStore formsStore;
    private final Clock clock;

    public StudentFormsService(
            StudentProfileStore studentProfileStore,
            StudentFormsStore formsStore,
            Clock clock) {
        this.studentProfileStore = studentProfileStore;
        this.formsStore = formsStore;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StudentFormSummary> getForms(String authenticatedUserId, StudentFormStatus status) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        return formsStore.findByStudent(student.id(), status);
    }

    @Transactional(readOnly = true)
    public StudentFormDetails getForm(String authenticatedUserId, UUID formId) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        return formsStore.findByStudentAndId(student.id(), formId)
                .orElseThrow(FormsException::formNotFound);
    }

    @Transactional
    public StudentFormDetails createForm(
            String authenticatedUserId,
            StudentFormType type,
            String reason,
            LocalDate startsOn,
            LocalDate endsOn) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        if (type == null || reason == null || reason.isBlank() || reason.length() > 1000) {
            throw FormsException.invalidPayload();
        }
        validateDates(type, startsOn, endsOn);
        UUID formId = UUID.randomUUID();
        Instant submittedAt = clock.instant();
        formsStore.create(
                formId,
                student.id(),
                type,
                reason.trim(),
                startsOn,
                endsOn,
                submittedAt);
        return formsStore.findByStudentAndId(student.id(), formId)
                .orElseThrow(FormsException::formNotFound);
    }

    @Transactional
    public StudentFormDetails cancelForm(String authenticatedUserId, UUID formId) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        StudentFormSummary form = formsStore.lockByStudentAndId(student.id(), formId)
                .orElseThrow(FormsException::formNotFound);
        if (!form.status().canBeCancelledByStudent()) {
            throw FormsException.cannotCancel();
        }
        Instant cancelledAt = clock.instant();
        formsStore.cancel(form.id(), cancelledAt);
        return formsStore.findByStudentAndId(student.id(), formId)
                .orElseThrow(FormsException::formNotFound);
    }

    private static void validateDates(
            StudentFormType type,
            LocalDate startsOn,
            LocalDate endsOn) {
        try {
            StudentFormSummary.validateDates(type, startsOn, endsOn);
        } catch (IllegalArgumentException exception) {
            throw FormsException.invalidDates();
        }
    }

    private StudentProfile authenticatedStudent(String authenticatedUserId) {
        UUID userId = parseAuthenticatedUserId(authenticatedUserId);
        return studentProfileStore.findByUserId(userId)
                .orElseThrow(FormsException::studentProfileNotFound);
    }

    private static UUID parseAuthenticatedUserId(String authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw FormsException.invalidAuthenticatedSubject();
        }
        try {
            return UUID.fromString(authenticatedUserId);
        } catch (IllegalArgumentException exception) {
            throw FormsException.invalidAuthenticatedSubject();
        }
    }
}
