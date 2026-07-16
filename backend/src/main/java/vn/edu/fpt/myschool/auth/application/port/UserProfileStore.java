package vn.edu.fpt.myschool.auth.application.port;

import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.auth.domain.UserProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;

/**
 * Resolves the profile a user holds for one specific role.
 *
 * <p>An account may hold several roles, so a profile is only meaningful once the active role is
 * known. The user id always comes from the authenticated principal, never from the client.
 */
public interface UserProfileStore {

    Optional<UserProfile> findProfile(UUID userId, UserRole role);
}
