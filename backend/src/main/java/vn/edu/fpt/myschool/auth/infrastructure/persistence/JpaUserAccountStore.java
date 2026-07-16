package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.UserAccountStore;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserRole;

@Repository
class JpaUserAccountStore implements UserAccountStore {

    private final UserJpaRepository userRepository;
    private final UserRoleJpaRepository userRoleRepository;

    JpaUserAccountStore(
            UserJpaRepository userRepository,
            UserRoleJpaRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public Optional<UserAccount> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByPhoneNumberForUpdate(String phoneNumber) {
        return userRepository.findByPhoneNumberForUpdate(phoneNumber).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return userRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByIdForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId).map(this::toDomain);
    }

    @Override
    public void updatePassword(UUID userId, String passwordHash, Instant updatedAt) {
        if (userRepository.updatePassword(userId, passwordHash, updatedAt) != 1) {
            throw new IllegalStateException("User account no longer exists");
        }
    }

    private UserAccount toDomain(UserJpaEntity user) {
        Set<UserRole> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(UserRoleJpaEntity::getRole)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
        return new UserAccount(
                user.getId(),
                user.getPhoneNumber(),
                user.getPasswordHash(),
                roles,
                user.isEnabled());
    }
}
