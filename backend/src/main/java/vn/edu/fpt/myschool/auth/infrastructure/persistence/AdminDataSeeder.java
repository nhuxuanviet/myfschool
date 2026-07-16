package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.domain.UserRole;
import vn.edu.fpt.myschool.auth.domain.VietnamesePhoneNumber;

@Component
@Profile("(dev | e2e) & !prod")
@Order(110)
class AdminDataSeeder implements ApplicationRunner {

    private static final UUID ADMIN_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000121");
    private static final UUID ADMIN_PROFILE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000221");

    private final UserJpaRepository userRepository;
    private final AdminProfileJpaRepository adminProfileRepository;
    private final UserRoleGranter userRoleGranter;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;
    private final Clock clock;

    AdminDataSeeder(
            UserJpaRepository userRepository,
            AdminProfileJpaRepository adminProfileRepository,
            UserRoleGranter userRoleGranter,
            PasswordEncoder passwordEncoder,
            AdminSeedProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.userRoleGranter = userRoleGranter;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String phoneNumber = VietnamesePhoneNumber.normalize(properties.phoneNumber()).value();
        UserJpaEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createUser(phoneNumber));
        // A user that already exists under this phone but holds other roles belongs to someone
        // else; granting admin to them would quietly escalate a real account. A newly created
        // user holds no role yet, which is why an empty set is allowed through.
        Set<UserRole> existingRoles = userRoleGranter.rolesOf(user.getId());
        if (!existingRoles.isEmpty() && !existingRoles.contains(UserRole.ADMIN)) {
            throw new IllegalStateException("Admin seed phone belongs to a non-admin user");
        }
        userRoleGranter.grant(user.getId(), UserRole.ADMIN);
        synchronizePassword(user);
        adminProfileRepository.findByUserId(user.getId()).ifPresentOrElse(
                profile -> profile.updateFullName(properties.fullName(), clock.instant()),
                () -> createProfile(user.getId()));
    }

    private UserJpaEntity createUser(String phoneNumber) {
        Instant now = clock.instant();
        return userRepository.save(new UserJpaEntity(
                ADMIN_USER_ID,
                phoneNumber,
                passwordEncoder.encode(properties.password()),
                true,
                now));
    }

    private void createProfile(UUID userId) {
        adminProfileRepository.save(new AdminProfileJpaEntity(
                ADMIN_PROFILE_ID,
                userId,
                properties.fullName(),
                clock.instant()));
    }

    private void synchronizePassword(UserJpaEntity user) {
        if (!passwordEncoder.matches(properties.password(), user.getPasswordHash())) {
            user.updatePassword(passwordEncoder.encode(properties.password()), clock.instant());
        }
    }
}
