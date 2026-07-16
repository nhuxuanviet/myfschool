package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AdminProfileJpaRepository extends JpaRepository<AdminProfileJpaEntity, UUID> {

    Optional<AdminProfileJpaEntity> findByUserId(UUID userId);
}
