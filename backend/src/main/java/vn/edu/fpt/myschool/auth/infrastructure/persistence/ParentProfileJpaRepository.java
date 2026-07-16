package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ParentProfileJpaRepository extends JpaRepository<ParentProfileJpaEntity, UUID> {

    Optional<ParentProfileJpaEntity> findByUserId(UUID userId);
}
