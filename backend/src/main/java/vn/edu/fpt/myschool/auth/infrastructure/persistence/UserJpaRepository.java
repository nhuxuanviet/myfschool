package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByPhoneNumber(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserJpaEntity user where user.phoneNumber = :phoneNumber")
    Optional<UserJpaEntity> findByPhoneNumberForUpdate(
            @Param("phoneNumber") String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserJpaEntity user where user.id = :userId")
    Optional<UserJpaEntity> findByIdForUpdate(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserJpaEntity user
            set user.passwordHash = :passwordHash,
                user.credentialsUpdatedAt = :updatedAt,
                user.updatedAt = :updatedAt
            where user.id = :userId
            """)
    int updatePassword(
            @Param("userId") UUID userId,
            @Param("passwordHash") String passwordHash,
            @Param("updatedAt") Instant updatedAt);
}
