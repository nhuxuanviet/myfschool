package vn.edu.fpt.myschool.parent.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A child a guardian may see.
 *
 * @param relationship FATHER, MOTHER or GUARDIAN
 * @param contactOrder who the school calls first
 */
public record ParentChild(
        UUID studentId,
        String studentCode,
        String fullName,
        int gradeLevel,
        String className,
        String relationship,
        int contactOrder) {

    public ParentChild {
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(relationship, "relationship must not be null");
    }
}
