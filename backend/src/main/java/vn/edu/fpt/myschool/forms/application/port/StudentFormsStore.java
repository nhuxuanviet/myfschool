package vn.edu.fpt.myschool.forms.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.forms.domain.StudentFormDetails;
import vn.edu.fpt.myschool.forms.domain.StudentFormStatus;
import vn.edu.fpt.myschool.forms.domain.StudentFormSummary;
import vn.edu.fpt.myschool.forms.domain.StudentFormType;

public interface StudentFormsStore {

    List<StudentFormSummary> findByStudent(UUID studentId, StudentFormStatus status);

    Optional<StudentFormDetails> findByStudentAndId(UUID studentId, UUID formId);

    Optional<StudentFormSummary> lockByStudentAndId(UUID studentId, UUID formId);

    void create(
            UUID formId,
            UUID studentId,
            StudentFormType type,
            String reason,
            LocalDate startsOn,
            LocalDate endsOn,
            Instant submittedAt);

    void cancel(UUID formId, Instant cancelledAt);
}
