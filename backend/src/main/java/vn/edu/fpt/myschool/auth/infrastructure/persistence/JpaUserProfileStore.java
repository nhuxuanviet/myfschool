package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.UserProfileStore;
import vn.edu.fpt.myschool.auth.domain.AdminProfile;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.auth.domain.UserProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;

@Repository
class JpaUserProfileStore implements UserProfileStore {

    private final StudentJpaRepository studentRepository;
    private final AdminProfileJpaRepository adminProfileRepository;

    JpaUserProfileStore(
            StudentJpaRepository studentRepository,
            AdminProfileJpaRepository adminProfileRepository) {
        this.studentRepository = studentRepository;
        this.adminProfileRepository = adminProfileRepository;
    }

    @Override
    public Optional<UserProfile> findProfile(UUID userId, UserRole role) {
        return switch (role) {
            case STUDENT -> studentRepository.findByUserId(userId).map(JpaUserProfileStore::toStudent);
            case ADMIN -> adminProfileRepository.findByUserId(userId).map(JpaUserProfileStore::toAdmin);
            // Teacher and parent profiles arrive with their own tables later in R1.
            case TEACHER, PARENT -> Optional.empty();
        };
    }

    private static UserProfile toStudent(StudentJpaEntity student) {
        return new StudentProfile(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                student.getGradeLevel(),
                student.getClassName());
    }

    private static UserProfile toAdmin(AdminProfileJpaEntity admin) {
        return new AdminProfile(admin.getId(), admin.getFullName());
    }
}
