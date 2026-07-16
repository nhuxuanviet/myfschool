package vn.edu.fpt.myschool.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String phoneNumber,
        String passwordHash,
        UserRole role,
        boolean enabled,
        UserProfile profile) {

    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        if (role == UserRole.STUDENT && !(profile instanceof StudentProfile)) {
            throw new IllegalArgumentException("Student users require a student profile");
        }
        if (role == UserRole.ADMIN && !(profile instanceof AdminProfile)) {
            throw new IllegalArgumentException("Admin users require an admin profile");
        }
    }

    public StudentProfile student() {
        if (profile instanceof StudentProfile studentProfile) {
            return studentProfile;
        }
        throw new IllegalStateException("User account is not a student");
    }

    public AdminProfile admin() {
        if (profile instanceof AdminProfile adminProfile) {
            return adminProfile;
        }
        throw new IllegalStateException("User account is not an admin");
    }
}
