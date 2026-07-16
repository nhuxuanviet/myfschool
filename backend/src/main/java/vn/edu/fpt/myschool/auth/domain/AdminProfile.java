package vn.edu.fpt.myschool.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record AdminProfile(UUID id, String fullName) implements UserProfile {

    public AdminProfile {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(fullName, "fullName must not be null");
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
    }
}
