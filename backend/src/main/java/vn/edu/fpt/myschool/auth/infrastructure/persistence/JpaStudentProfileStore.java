package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;

@Repository
class JpaStudentProfileStore implements StudentProfileStore {

    private final StudentJpaRepository studentRepository;

    JpaStudentProfileStore(StudentJpaRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public Optional<StudentProfile> findByUserId(UUID userId) {
        return studentRepository.findByUserId(userId).map(this::toDomain);
    }

    private StudentProfile toDomain(StudentJpaEntity student) {
        return new StudentProfile(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                student.getGradeLevel(),
                student.getClassName());
    }
}
