package vn.edu.fpt.myschool.admin.identity.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Read models for the identity records an administrator maintains. */
public final class AdminIdentity {

    private AdminIdentity() {
    }

    /**
     * @param userId null while the school has entered the teacher but not yet issued an account.
     *     Such a teacher can still be assigned and appear on a timetable; they just cannot sign in.
     */
    public record Teacher(
            UUID id,
            UUID userId,
            String teacherCode,
            String fullName,
            String email,
            String phoneNumber,
            boolean enabled,
            long version) {

        public Teacher {
            Objects.requireNonNull(id, "id must not be null");
            requireText(teacherCode, "teacherCode");
            requireText(fullName, "fullName");
        }

        public boolean hasAccount() {
            return userId != null;
        }
    }

    public record TeacherPage(List<Teacher> items, int page, int size, long totalElements) {

        public TeacherPage {
            items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
            if (page < 0 || size <= 0 || totalElements < 0) {
                throw new IllegalArgumentException("page, size and totalElements must be non-negative");
            }
        }

        public int totalPages() {
            return (int) Math.ceil((double) totalElements / size);
        }
    }

    /** @param userId null while the guardian has been entered but not given an account. */
    public record Parent(
            UUID id,
            UUID userId,
            String fullName,
            String email,
            String phoneNumber,
            boolean enabled,
            long version,
            int linkedStudents) {

        public Parent {
            Objects.requireNonNull(id, "id must not be null");
            requireText(fullName, "fullName");
            if (linkedStudents < 0) {
                throw new IllegalArgumentException("linkedStudents must not be negative");
            }
        }

        public boolean hasAccount() {
            return userId != null;
        }
    }

    public record ParentPage(List<Parent> items, int page, int size, long totalElements) {

        public ParentPage {
            items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
            if (page < 0 || size <= 0 || totalElements < 0) {
                throw new IllegalArgumentException("page, size and totalElements must be non-negative");
            }
        }

        public int totalPages() {
            return (int) Math.ceil((double) totalElements / size);
        }
    }

    public enum Relationship {
        FATHER,
        MOTHER,
        GUARDIAN
    }

    /**
     * @param effectiveTo the first day the link no longer applies, or null while it is in force.
     *     A link that starts and ends on the same day granted nothing, which is what an immediate
     *     revocation looks like. Ended links are kept rather than deleted, because who could see a
     *     child's data, and until when, is an accountability record.
     */
    public record GuardianLink(
            UUID id,
            UUID parentId,
            String parentFullName,
            UUID studentId,
            String studentFullName,
            String studentCode,
            Relationship relationship,
            int contactOrder,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {

        public GuardianLink {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(parentId, "parentId must not be null");
            Objects.requireNonNull(studentId, "studentId must not be null");
            Objects.requireNonNull(relationship, "relationship must not be null");
            Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
            if (contactOrder < 1) {
                throw new IllegalArgumentException("contactOrder must be at least 1");
            }
            if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                throw new IllegalArgumentException("effectiveTo must not precede effectiveFrom");
            }
        }

        public boolean isInForce() {
            return effectiveTo == null;
        }
    }

    static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
