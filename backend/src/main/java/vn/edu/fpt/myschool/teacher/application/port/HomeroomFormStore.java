package vn.edu.fpt.myschool.teacher.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.teacher.domain.HomeroomForm;

/**
 * Leave requests raised by the students of a homeroom class.
 *
 * <p>Only leave-of-absence forms travel this way. Administrative forms are the office's business
 * and never appear here, whatever the form id says.
 */
public interface HomeroomFormStore {

    List<HomeroomForm> findLeaveFormsForHomeroom(UUID teacherId, String status);

    Optional<HomeroomForm> findLeaveForm(UUID formId);

    /** True when this teacher is the homeroom teacher of the class the form's student belongs to. */
    boolean isHomeroomTeacherOfFormStudent(UUID teacherId, UUID formId);

    boolean updateStatus(UUID formId, String status, Instant now);

    void appendHistory(
            UUID formId, String status, UUID actorUserId, String note, Instant occurredAt);
}
