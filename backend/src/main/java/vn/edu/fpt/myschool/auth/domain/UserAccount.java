package vn.edu.fpt.myschool.auth.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * An authenticated identity and the roles it holds.
 *
 * <p>Profiles are deliberately absent. One account may hold several roles, so there is no single
 * profile that belongs to it; each role's profile is resolved from its own store once the active
 * role is known.
 */
public record UserAccount(
        UUID id,
        String phoneNumber,
        String passwordHash,
        Set<UserRole> roles,
        boolean enabled) {

    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("User account requires at least one role");
        }
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
}
