package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserRoleJpaRepository
        extends JpaRepository<UserRoleJpaEntity, UserRoleJpaEntity.UserRoleId> {

    List<UserRoleJpaEntity> findByUserId(UUID userId);
}
