package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.UserProfileStore;
import vn.edu.fpt.myschool.auth.domain.AdminProfile;
import vn.edu.fpt.myschool.auth.domain.ParentProfile;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.auth.domain.TeacherProfile;
import vn.edu.fpt.myschool.auth.domain.UserProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;

@Repository
class JpaUserProfileStore implements UserProfileStore {

    private final StudentJpaRepository studentRepository;
    private final TeacherProfileJpaRepository teacherRepository;
    private final ParentProfileJpaRepository parentRepository;
    private final AdminProfileJpaRepository adminProfileRepository;

    JpaUserProfileStore(
            StudentJpaRepository studentRepository,
            TeacherProfileJpaRepository teacherRepository,
            ParentProfileJpaRepository parentRepository,
            AdminProfileJpaRepository adminProfileRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.parentRepository = parentRepository;
        this.adminProfileRepository = adminProfileRepository;
    }

    /**
     * A disabled teacher or parent profile resolves to nothing.
     *
     * <p>Retiring a teacher must take effect even while their user account stays enabled, so the
     * profile's own flag is what decides here.
     */
    @Override
    public Optional<UserProfile> findProfile(UUID userId, UserRole role) {
        return switch (role) {
            case STUDENT -> studentRepository.findByUserId(userId).map(JpaUserProfileStore::toStudent);
            case TEACHER -> teacherRepository.findByUserId(userId)
                    .filter(TeacherProfileJpaEntity::isEnabled)
                    .map(JpaUserProfileStore::toTeacher);
            case PARENT -> parentRepository.findByUserId(userId)
                    .filter(ParentProfileJpaEntity::isEnabled)
                    .map(JpaUserProfileStore::toParent);
            case ADMIN -> adminProfileRepository.findByUserId(userId).map(JpaUserProfileStore::toAdmin);
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

    private static UserProfile toTeacher(TeacherProfileJpaEntity teacher) {
        return new TeacherProfile(teacher.getId(), teacher.getTeacherCode(), teacher.getFullName());
    }

    private static UserProfile toParent(ParentProfileJpaEntity parent) {
        return new ParentProfile(parent.getId(), parent.getFullName());
    }

    private static UserProfile toAdmin(AdminProfileJpaEntity admin) {
        return new AdminProfile(admin.getId(), admin.getFullName());
    }
}
