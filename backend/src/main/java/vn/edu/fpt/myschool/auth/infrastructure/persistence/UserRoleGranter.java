package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.domain.UserRole;

/**
 * Grants a role to a user account.
 *
 * <p>Every path that creates a user account must maintain the {@code user_roles} rows, because
 * that table is the single source of truth for authorisation once {@code users.role} is dropped.
 */
@Component
class UserRoleGranter {

    private final UserRoleJpaRepository userRoleRepository;
    private final Clock clock;

    UserRoleGranter(UserRoleJpaRepository userRoleRepository, Clock clock) {
        this.userRoleRepository = userRoleRepository;
        this.clock = clock;
    }

    void grant(UUID userId, UserRole role) {
        if (userRoleRepository.existsById(new UserRoleJpaEntity.UserRoleId(userId, role))) {
            return;
        }
        userRoleRepository.save(new UserRoleJpaEntity(userId, role, clock.instant()));
    }
}
