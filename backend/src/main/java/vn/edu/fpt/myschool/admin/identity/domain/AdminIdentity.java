package vn.edu.fpt.myschool.admin.identity.domain;

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

    public record TeacherPage(List<Teacher> items, int page, int size, long totalItems) {

        public TeacherPage {
            items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
            if (page < 0 || size <= 0 || totalItems < 0) {
                throw new IllegalArgumentException("page, size and totalItems must be non-negative");
            }
        }

        public int totalPages() {
            return (int) Math.ceil((double) totalItems / size);
        }
    }

    static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
