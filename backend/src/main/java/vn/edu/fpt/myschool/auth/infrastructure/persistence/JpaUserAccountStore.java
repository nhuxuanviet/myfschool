package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.UserAccountStore;
import vn.edu.fpt.myschool.auth.domain.AdminProfile;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;

@Repository
class JpaUserAccountStore implements UserAccountStore {

    private final UserJpaRepository userRepository;
    private final StudentJpaRepository studentRepository;
    private final AdminProfileJpaRepository adminProfileRepository;

    JpaUserAccountStore(
            UserJpaRepository userRepository,
            StudentJpaRepository studentRepository,
            AdminProfileJpaRepository adminProfileRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.adminProfileRepository = adminProfileRepository;
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
        UserProfile profile = switch (user.getRole()) {
            case STUDENT -> toStudentProfile(user.getId());
            case ADMIN -> toAdminProfile(user.getId());
        };
        return new UserAccount(
                user.getId(),
                user.getPhoneNumber(),
                user.getPasswordHash(),
                user.getRole(),
                user.isEnabled(),
                profile);
    }

    private StudentProfile toStudentProfile(UUID userId) {
        StudentJpaEntity student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Student profile is missing for the user account"));
        return new StudentProfile(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                student.getGradeLevel(),
                student.getClassName());
    }

    private AdminProfile toAdminProfile(UUID userId) {
        AdminProfileJpaEntity admin = adminProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Admin profile is missing for the user account"));
        return new AdminProfile(admin.getId(), admin.getFullName());
    }
}
