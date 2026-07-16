package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TeacherProfileJpaRepository extends JpaRepository<TeacherProfileJpaEntity, UUID> {

    Optional<TeacherProfileJpaEntity> findByUserId(UUID userId);
}
