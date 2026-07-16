package vn.edu.fpt.myschool.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record ParentProfile(UUID id, String fullName) implements UserProfile {

    public ParentProfile {
        Objects.requireNonNull(id, "id must not be null");
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
    }
}
