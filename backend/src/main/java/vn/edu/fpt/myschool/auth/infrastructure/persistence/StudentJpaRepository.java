package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface StudentJpaRepository extends JpaRepository<StudentJpaEntity, UUID> {

    Optional<StudentJpaEntity> findByUserId(UUID userId);
}
