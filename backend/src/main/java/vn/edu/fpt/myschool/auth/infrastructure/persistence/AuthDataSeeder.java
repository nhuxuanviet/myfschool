package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Clock;
import java.time.Instant;
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
@Order(100)
class AuthDataSeeder implements ApplicationRunner {

    private static final UUID SEEDED_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SEEDED_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PEER_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID PEER_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000211");
    private static final String PEER_PHONE_NUMBER = "0977777777";
    private static final String PEER_STUDENT_CODE = "SE1913011";
    private static final String PEER_FULL_NAME = "Trần Gia Bảo";
    private static final int PEER_GRADE_LEVEL = 11;
    private static final String PEER_CLASS_NAME = "11A1";

    private final UserJpaRepository userRepository;
    private final StudentJpaRepository studentRepository;
    private final UserRoleGranter userRoleGranter;
    private final PasswordEncoder passwordEncoder;
    private final AuthSeedProperties properties;
    private final Clock clock;

    AuthDataSeeder(
            UserJpaRepository userRepository,
            StudentJpaRepository studentRepository,
            UserRoleGranter userRoleGranter,
            PasswordEncoder passwordEncoder,
            AuthSeedProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
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
        userRoleGranter.grant(user.getId(), UserRole.STUDENT);
        synchronizeSeedPassword(user);
        studentRepository.findByUserId(user.getId()).ifPresentOrElse(
                this::updateStudent,
                () -> createStudent(user.getId()));
        seedPeerStudent();
    }

    private UserJpaEntity createUser(String phoneNumber) {
        Instant now = clock.instant();
        return userRepository.save(new UserJpaEntity(
                SEEDED_USER_ID,
                phoneNumber,
                passwordEncoder.encode(properties.password()),
                UserRole.STUDENT,
                true,
                now));
    }

    private void createStudent(UUID userId) {
        studentRepository.save(new StudentJpaEntity(
                SEEDED_STUDENT_ID,
                userId,
                properties.studentCode(),
                properties.fullName(),
                properties.gradeLevel(),
                properties.className(),
                clock.instant()));
    }

    private void updateStudent(StudentJpaEntity student) {
        student.updateSeedProfile(
                properties.studentCode(),
                properties.fullName(),
                properties.gradeLevel(),
                properties.className(),
                clock.instant());
    }

    private void synchronizeSeedPassword(UserJpaEntity user) {
        if (!passwordEncoder.matches(properties.password(), user.getPasswordHash())) {
            user.updatePassword(passwordEncoder.encode(properties.password()), clock.instant());
        }
    }

    private void seedPeerStudent() {
        UserJpaEntity user = userRepository.findByPhoneNumber(PEER_PHONE_NUMBER)
                .orElseGet(this::createPeerUser);
        userRoleGranter.grant(user.getId(), UserRole.STUDENT);
        synchronizeSeedPassword(user);
        if (studentRepository.findByUserId(user.getId()).isEmpty()) {
            studentRepository.save(new StudentJpaEntity(
                    PEER_STUDENT_ID,
                    user.getId(),
                    PEER_STUDENT_CODE,
                    PEER_FULL_NAME,
                    PEER_GRADE_LEVEL,
                    PEER_CLASS_NAME,
                    clock.instant()));
        }
    }

    private UserJpaEntity createPeerUser() {
        Instant now = clock.instant();
        return userRepository.save(new UserJpaEntity(
                PEER_USER_ID,
                PEER_PHONE_NUMBER,
                passwordEncoder.encode(properties.password()),
                UserRole.STUDENT,
                true,
                now));
    }
}
