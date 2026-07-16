package vn.edu.fpt.myschool.auth.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.auth.domain.UserAccount;

public interface UserAccountStore {

    Optional<UserAccount> findByPhoneNumber(String phoneNumber);

    Optional<UserAccount> findByPhoneNumberForUpdate(String phoneNumber);

    Optional<UserAccount> findById(UUID userId);

    Optional<UserAccount> findByIdForUpdate(UUID userId);

    void updatePassword(UUID userId, String passwordHash, Instant updatedAt);
}
