package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.domain.UserRole;

/**
 * Grants a role to a user account.
 *
 * <p>Every path that creates a user account must write its {@code user_roles} rows in the same
 * transaction: that table is the only source of truth for authorisation, and an account holding
 * no role cannot be loaded at all.
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

    Set<UserRole> rolesOf(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRoleJpaEntity::getRole)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
    }
}
