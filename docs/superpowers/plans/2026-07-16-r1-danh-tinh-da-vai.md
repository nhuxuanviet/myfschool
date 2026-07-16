# R1 — Danh tính đa vai trò: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cho phép một tài khoản mang nhiều vai (STUDENT/TEACHER/PARENT/ADMIN), có hồ sơ giáo viên - phụ huynh và liên kết giám hộ, JWT gắn vai đang hoạt động, và chuyển API học sinh sang `/api/v1/student/**` mà không làm đỏ bộ hồi quy học sinh.

**Architecture:** Thêm bảng `user_roles` làm nguồn sự thật duy nhất về vai, thay cột `users.role`. `UserAccount` mang `Set<UserRole>` thay vì một vai. Đăng nhập trả về danh sách vai; client chọn vai; access token và refresh session gắn vai đang hoạt động. Hồ sơ giáo viên/phụ huynh tách khỏi `users` để nhà trường nhập trước, cấp tài khoản sau. Quyền vẫn `denyAll()` mặc định, thêm namespace `/api/v1/teacher/**` và `/api/v1/parent/**` nhưng R1 chưa có endpoint nghiệp vụ nào trong đó ngoài `me`.

**Tech Stack:** Spring Boot modular monolith, Flyway (V19-V21), PostgreSQL 18 qua Testcontainers, Nimbus JWT HS256, React + MUI + TanStack Query cho admin-web, Flutter + Riverpod + go_router.

**Thứ tự an toàn:** `users.role` chỉ bị xoá ở task cuối (Task 12), sau khi toàn bộ code đã đọc từ `user_roles`. Migration nào cũng phải giữ bộ hồi quy học sinh xanh.

---

## File Structure

**Migration**
- Create: `backend/src/main/resources/db/migration/V19__create_user_roles.sql` — bảng `user_roles`, backfill từ `users.role`, dựng lại partial index phụ thuộc `users.role`
- Create: `backend/src/main/resources/db/migration/V20__create_teacher_parent_profiles.sql` — `teacher_profiles`, `parent_profiles`, `parent_student_links`
- Create: `backend/src/main/resources/db/migration/V21__drop_users_role.sql` — xoá `users.role` và `ck_users_role`

**Domain auth**
- Modify: `backend/src/main/java/vn/edu/fpt/myschool/auth/domain/UserRole.java` — thêm TEACHER, PARENT
- Modify: `backend/src/main/java/vn/edu/fpt/myschool/auth/domain/UserAccount.java` — `Set<UserRole> roles` thay `UserRole role`
- Create: `backend/src/main/java/vn/edu/fpt/myschool/auth/domain/TeacherProfile.java`
- Create: `backend/src/main/java/vn/edu/fpt/myschool/auth/domain/ParentProfile.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myschool/auth/domain/UserProfile.java` — mở sealed cho hai profile mới

**Persistence auth**
- Create: `backend/.../auth/infrastructure/persistence/UserRoleJpaEntity.java`
- Create: `backend/.../auth/infrastructure/persistence/UserRoleJpaRepository.java`
- Create: `backend/.../auth/infrastructure/persistence/TeacherProfileJpaEntity.java` + `TeacherProfileJpaRepository.java`
- Create: `backend/.../auth/infrastructure/persistence/ParentProfileJpaEntity.java` + `ParentProfileJpaRepository.java`
- Modify: `backend/.../auth/infrastructure/persistence/JpaUserAccountStore.java` — đọc vai từ `user_roles`
- Modify: `backend/.../auth/infrastructure/persistence/UserJpaEntity.java` — bỏ field `role` ở Task 12

**Security**
- Modify: `backend/.../auth/infrastructure/security/JwtAccessTokenIssuer.java` — claim `role` = vai đang hoạt động
- Modify: `backend/.../auth/infrastructure/security/AuthSecurityConfiguration.java` — `/api/v1/student/**`, `/api/v1/teacher/**`, `/api/v1/parent/**`

**Test**
- Create: `backend/src/test/java/vn/edu/fpt/myschool/auth/MultiRoleIdentityIntegrationTest.java`
- Create: `backend/src/test/java/vn/edu/fpt/myschool/auth/RoleIsolationIntegrationTest.java` — kiểm thử phân quyền phủ định (spec mục 10)

---

## Task 1: Bảng `user_roles` và backfill

**Files:**
- Create: `backend/src/main/resources/db/migration/V19__create_user_roles.sql`
- Test: `backend/src/test/java/vn/edu/fpt/myschool/auth/UserRolesMigrationIntegrationTest.java`

Ràng buộc đã biết: V14 tạo `ix_users_student_enabled_phone ON users (enabled, phone_number, id) WHERE role = 'STUDENT'`. Partial index này phụ thuộc `users.role`, nên Task 12 không xoá được cột nếu index còn. V19 dựng index thay thế không phụ thuộc `users.role`.

- [ ] **Step 1: Viết migration**

```sql
-- V19__create_user_roles.sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_roles_role
        CHECK (role IN ('STUDENT', 'TEACHER', 'PARENT', 'ADMIN'))
);

-- Nguồn sự thật cũ là users.role; chuyển nguyên trạng, không suy diễn thêm vai.
INSERT INTO user_roles (user_id, role, created_at)
SELECT id, role, CURRENT_TIMESTAMP FROM users;

CREATE INDEX ix_user_roles_role_user ON user_roles (role, user_id);

-- V14 dựng ix_users_student_enabled_phone với điều kiện WHERE role = 'STUDENT'.
-- Cột users.role bị xoá ở V21 nên index đó phải nhường chỗ cho một index
-- tương đương dựa trên user_roles.
DROP INDEX ix_users_student_enabled_phone;

CREATE INDEX ix_user_roles_student_lookup
    ON user_roles (user_id)
    WHERE role = 'STUDENT';
```

- [ ] **Step 2: Viết kiểm thử migration**

```java
package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
class UserRolesMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void backfillsOneRoleRowForEveryExistingUser() {
        Integer users = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users", Integer.class);
        Integer roles = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_roles", Integer.class);
        assertThat(roles).isEqualTo(users);
    }

    @Test
    void seededStudentKeepsStudentRole() {
        List<String> roles = jdbcTemplate.queryForList(
                """
                SELECT ur.role FROM user_roles ur
                JOIN users u ON u.id = ur.user_id
                WHERE u.phone_number = '0912345678'
                """,
                String.class);
        assertThat(roles).containsExactly("STUDENT");
    }

    @Test
    void rejectsRoleOutsideTheAllowedSet() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO user_roles (user_id, role, created_at)
                        SELECT id, 'PRINCIPAL', CURRENT_TIMESTAMP FROM users LIMIT 1
                        """))
                .hasMessageContaining("ck_user_roles_role");
    }

    @Test
    void dropsTheIndexThatDependsOnUsersRole() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM pg_indexes
                WHERE indexname = 'ix_users_student_enabled_phone'
                """,
                Integer.class);
        assertThat(count).isZero();
    }
}
```

- [ ] **Step 3: Chạy kiểm thử, xác nhận đỏ trước khi có migration**

Run: `cd backend && ./mvnw test -Dtest=UserRolesMigrationIntegrationTest`
Expected: FAIL — `relation "user_roles" does not exist`

- [ ] **Step 4: Chạy lại sau khi thêm migration**

Run: `cd backend && ./mvnw test -Dtest=UserRolesMigrationIntegrationTest`
Expected: PASS, 4 test

- [ ] **Step 5: Chạy toàn bộ hồi quy backend**

Run: `cd backend && ./mvnw test`
Expected: PASS toàn bộ. V19 thuần additive nên không được làm đỏ test nào.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V19__create_user_roles.sql \
        backend/src/test/java/vn/edu/fpt/myschool/auth/UserRolesMigrationIntegrationTest.java
git commit -m "feat(auth): thêm bảng user_roles và backfill từ users.role"
```

---

## Task 2: Mở rộng enum `UserRole`

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myschool/auth/domain/UserRole.java`

- [ ] **Step 1: Sửa enum**

```java
package vn.edu.fpt.myschool.auth.domain;

public enum UserRole {
    STUDENT,
    TEACHER,
    PARENT,
    ADMIN
}
```

- [ ] **Step 2: Biên dịch để lộ mọi switch không còn đủ nhánh**

Run: `cd backend && ./mvnw -q compile`
Expected: FAIL. `JwtAccessTokenIssuer.issue` và `JpaUserAccountStore.toDomain` dùng switch exhaustive trên `UserRole`; thêm hằng số làm hai chỗ này mất tính đầy đủ. Đây là mục đích của bước này — trình biên dịch chỉ ra đúng các điểm phải sửa ở Task 3 và Task 5.

- [ ] **Step 3: Commit sau khi Task 3 và Task 5 xong**

Enum một mình không biên dịch được nên không commit riêng. Commit cùng Task 3.

---

## Task 3: `UserAccount` mang tập vai

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myschool/auth/domain/UserAccount.java`
- Test: `backend/src/test/java/vn/edu/fpt/myschool/auth/domain/UserAccountTest.java`

Bất biến cũ "STUDENT phải có StudentProfile" không còn diễn đạt được khi một tài khoản có nhiều vai và nhiều hồ sơ. Thay bằng: tập vai không rỗng, và mỗi vai có hồ sơ tương ứng được nạp riêng.

- [ ] **Step 1: Viết kiểm thử thất bại**

```java
package vn.edu.fpt.myschool.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void rejectsAnAccountWithoutAnyRole() {
        assertThatThrownBy(() -> new UserAccount(
                        USER_ID, "0912345678", "hash", Set.of(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one role");
    }

    @Test
    void exposesRolesAsAnImmutableSet() {
        UserAccount account = new UserAccount(
                USER_ID, "0912345678", "hash", Set.of(UserRole.TEACHER), true);
        assertThatThrownBy(() -> account.roles().add(UserRole.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void confirmsWhetherTheAccountHoldsARole() {
        UserAccount account = new UserAccount(
                USER_ID,
                "0912345678",
                "hash",
                Set.of(UserRole.TEACHER, UserRole.PARENT),
                true);
        assertThat(account.hasRole(UserRole.TEACHER)).isTrue();
        assertThat(account.hasRole(UserRole.ADMIN)).isFalse();
    }
}
```

- [ ] **Step 2: Chạy để xác nhận đỏ**

Run: `cd backend && ./mvnw test -Dtest=UserAccountTest`
Expected: FAIL — constructor chưa có chữ ký này.

- [ ] **Step 3: Viết `UserAccount`**

Hồ sơ bị tách khỏi `UserAccount`: mỗi vai có store riêng nạp hồ sơ khi cần. `UserAccount` chỉ còn danh tính và vai.

```java
package vn.edu.fpt.myschool.auth.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String phoneNumber,
        String passwordHash,
        Set<UserRole> roles,
        boolean enabled) {

    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("User account requires at least one role");
        }
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
}
```

- [ ] **Step 4: Chạy để xác nhận xanh**

Run: `cd backend && ./mvnw test -Dtest=UserAccountTest`
Expected: PASS, 3 test

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myschool/auth/domain/UserRole.java \
        backend/src/main/java/vn/edu/fpt/myschool/auth/domain/UserAccount.java \
        backend/src/test/java/vn/edu/fpt/myschool/auth/domain/UserAccountTest.java
git commit -m "feat(auth): UserAccount mang tập vai thay vì một vai duy nhất"
```

---

## Task 4: Đọc vai từ `user_roles`

**Files:**
- Create: `backend/.../auth/infrastructure/persistence/UserRoleJpaEntity.java`
- Create: `backend/.../auth/infrastructure/persistence/UserRoleJpaRepository.java`
- Modify: `backend/.../auth/infrastructure/persistence/JpaUserAccountStore.java`

- [ ] **Step 1: Viết entity**

```java
package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import vn.edu.fpt.myschool.auth.domain.UserRole;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleJpaEntity.UserRoleId.class)
class UserRoleJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserRoleJpaEntity() {
    }

    UserRoleJpaEntity(UUID userId, UserRole role, Instant createdAt) {
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    UUID getUserId() {
        return userId;
    }

    UserRole getRole() {
        return role;
    }

    record UserRoleId(UUID userId, UserRole role) implements java.io.Serializable {
        UserRoleId() {
            this(null, null);
        }
    }
}
```

- [ ] **Step 2: Viết repository**

```java
package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserRoleJpaRepository
        extends JpaRepository<UserRoleJpaEntity, UserRoleJpaEntity.UserRoleId> {

    List<UserRoleJpaEntity> findByUserId(UUID userId);
}
```

- [ ] **Step 3: Sửa `JpaUserAccountStore` đọc vai từ bảng mới**

```java
package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.UserAccountStore;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserRole;

@Repository
class JpaUserAccountStore implements UserAccountStore {

    private final UserJpaRepository userRepository;
    private final UserRoleJpaRepository userRoleRepository;

    JpaUserAccountStore(
            UserJpaRepository userRepository,
            UserRoleJpaRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public Optional<UserAccount> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByPhoneNumberForUpdate(String phoneNumber) {
        return userRepository.findByPhoneNumberForUpdate(phoneNumber).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return userRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByIdForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId).map(this::toDomain);
    }

    @Override
    public void updatePassword(UUID userId, String passwordHash, Instant updatedAt) {
        if (userRepository.updatePassword(userId, passwordHash, updatedAt) != 1) {
            throw new IllegalStateException("User account no longer exists");
        }
    }

    private UserAccount toDomain(UserJpaEntity user) {
        Set<UserRole> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(UserRoleJpaEntity::getRole)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
        return new UserAccount(
                user.getId(),
                user.getPhoneNumber(),
                user.getPasswordHash(),
                roles,
                user.isEnabled());
    }
}
```

- [ ] **Step 4: Chạy hồi quy auth**

Run: `cd backend && ./mvnw test -Dtest='Auth*IntegrationTest'`
Expected: PASS. Nếu đỏ vì `AuthService` còn so sánh `account.role()`, sửa ở Task 5 rồi chạy lại.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myschool/auth/infrastructure/persistence/
git commit -m "feat(auth): nạp vai từ bảng user_roles"
```

---

## Task 5: Vai đang hoạt động trong phiên và JWT

**Files:**
- Modify: `backend/.../auth/application/AuthService.java`
- Modify: `backend/.../auth/application/port/AccessTokenIssuer.java`
- Modify: `backend/.../auth/infrastructure/security/JwtAccessTokenIssuer.java`
- Modify: `backend/src/main/resources/db/migration/V19__create_user_roles.sql` — thêm `refresh_sessions.active_role`
- Test: `backend/src/test/java/vn/edu/fpt/myschool/auth/MultiRoleIdentityIntegrationTest.java`

Access token gắn vai đang hoạt động (spec mục 8.5). Refresh session cũng phải gắn vai, nếu không refresh sẽ không biết cấp lại token cho vai nào.

- [ ] **Step 1: Thêm cột vào V19 (migration chưa phát hành nên sửa tại chỗ hợp lệ)**

Chèn vào cuối `V19__create_user_roles.sql`:

```sql
-- Refresh session phải nhớ vai đang hoạt động, nếu không refresh không biết
-- cấp lại access token cho vai nào. Phiên cũ đều là học sinh hoặc admin;
-- suy ra từ user_roles vì mỗi tài khoản hiện chỉ có đúng một vai.
ALTER TABLE refresh_sessions ADD COLUMN active_role VARCHAR(32);

UPDATE refresh_sessions rs
SET active_role = ur.role
FROM user_roles ur
WHERE ur.user_id = rs.user_id;

ALTER TABLE refresh_sessions ALTER COLUMN active_role SET NOT NULL;

ALTER TABLE refresh_sessions
    ADD CONSTRAINT ck_refresh_sessions_active_role
    CHECK (active_role IN ('STUDENT', 'TEACHER', 'PARENT', 'ADMIN'));
```

- [ ] **Step 2: Viết kiểm thử thất bại**

```java
package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class MultiRoleIdentityIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void loginReturnsAvailableRolesAndTokenCarriesTheActiveRole() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"0912345678","password":"Student@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableRoles").isArray())
                .andExpect(jsonPath("$.activeRole").value("STUDENT"))
                .andReturn();

        String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        assertThat(jwtDecoder.decode(accessToken).getClaimAsString("role")).isEqualTo("STUDENT");
    }
}
```

- [ ] **Step 3: Chạy để xác nhận đỏ**

Run: `cd backend && ./mvnw test -Dtest=MultiRoleIdentityIntegrationTest`
Expected: FAIL — response chưa có `availableRoles`/`activeRole`.

- [ ] **Step 4: Sửa `AccessTokenIssuer` nhận vai**

```java
IssuedAccessToken issue(UserAccount userAccount, UserRole activeRole);
```

- [ ] **Step 5: Sửa `JwtAccessTokenIssuer`**

Claim `role` là vai đang hoạt động, không phải toàn bộ tập vai — tầng security dùng một chuỗi (`setAuthoritiesClaimName("role")`), và spec mục 8.5 quy định token gắn đúng một vai.

```java
@Override
public IssuedAccessToken issue(UserAccount userAccount, UserRole activeRole) {
    if (!userAccount.hasRole(activeRole)) {
        throw new IllegalArgumentException("Account does not hold the requested role");
    }
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(userAccount.id().toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("role", activeRole.name())
            .build();
    JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    String token = jwtEncoder
            .encode(JwtEncoderParameters.from(headers, claims))
            .getTokenValue();
    return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
}
```

Claim `studentId`/`adminId` bị bỏ. Lý do: spec mục 3 quy định định danh nghiệp vụ phải phân giải từ principal đã xác thực qua store, không lấy từ token. Giữ `studentId` trong token là mời gọi code tin token thay vì tra quan hệ.

- [ ] **Step 6: Sửa `AuthService.loginForRole` chọn vai**

```java
@Transactional(noRollbackFor = AuthException.class)
public AuthenticationResult loginForRole(
        String phoneNumber, String password, UserRole expectedRole) {
    // ... phần kiểm tra mật khẩu giữ nguyên ...
    if (account.isEmpty()
            || !passwordLengthValid
            || !passwordMatches
            || !account.orElseThrow().enabled()
            || !account.orElseThrow().hasRole(expectedRole)) {
        throw AuthException.invalidCredentials();
    }
    return issueSession(account.orElseThrow(), expectedRole, UUID.randomUUID(), null);
}
```

`issueSession` nhận thêm `UserRole activeRole`, ghi vào `RefreshSession.activeRole` và truyền cho `accessTokenIssuer.issue(account, activeRole)`.

- [ ] **Step 7: Chạy kiểm thử**

Run: `cd backend && ./mvnw test -Dtest=MultiRoleIdentityIntegrationTest`
Expected: PASS

- [ ] **Step 8: Chạy toàn bộ hồi quy backend**

Run: `cd backend && ./mvnw test`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add backend/
git commit -m "feat(auth): access token và refresh session gắn vai đang hoạt động"
```

---

## Task 6: Hồ sơ giáo viên, phụ huynh và liên kết giám hộ

**Files:**
- Create: `backend/src/main/resources/db/migration/V20__create_teacher_parent_profiles.sql`
- Test: `backend/src/test/java/vn/edu/fpt/myschool/auth/GuardianLinkMigrationIntegrationTest.java`

- [ ] **Step 1: Viết migration**

```sql
-- V20__create_teacher_parent_profiles.sql

-- user_id cho phép NULL: nhà trường nhập danh sách giáo viên trước, cấp tài
-- khoản sau. PostgreSQL cho phép nhiều NULL trong ràng buộc UNIQUE nên hồ sơ
-- chưa có tài khoản không xung đột nhau.
CREATE TABLE teacher_profiles (
    id UUID PRIMARY KEY,
    user_id UUID,
    teacher_code VARCHAR(32) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_teacher_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_teacher_profiles_code UNIQUE (teacher_code),
    CONSTRAINT fk_teacher_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_teacher_profiles_code CHECK (btrim(teacher_code) <> ''),
    CONSTRAINT ck_teacher_profiles_full_name CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_teacher_profiles_version CHECK (version >= 0)
);

CREATE TABLE parent_profiles (
    id UUID PRIMARY KEY,
    user_id UUID,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_parent_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_parent_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_parent_profiles_full_name CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_parent_profiles_version CHECK (version >= 0)
);

CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL,
    student_id UUID NOT NULL,
    relationship VARCHAR(32) NOT NULL,
    contact_order SMALLINT NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_parent_student_links_parent FOREIGN KEY (parent_id)
        REFERENCES parent_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_parent_student_links_student FOREIGN KEY (student_id)
        REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT ck_parent_student_links_relationship
        CHECK (relationship IN ('FATHER', 'MOTHER', 'GUARDIAN')),
    CONSTRAINT ck_parent_student_links_contact_order CHECK (contact_order >= 1),
    CONSTRAINT ck_parent_student_links_effective_range
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- Một cặp phụ huynh - học sinh chỉ được có một liên kết đang hiệu lực. Liên
-- kết đã kết thúc giữ lại để truy vết nên không dùng UNIQUE trên cả cặp.
CREATE UNIQUE INDEX uk_parent_student_links_active
    ON parent_student_links (parent_id, student_id)
    WHERE effective_to IS NULL;

CREATE INDEX ix_parent_student_links_student
    ON parent_student_links (student_id, contact_order);
CREATE INDEX ix_parent_student_links_parent_active
    ON parent_student_links (parent_id)
    WHERE effective_to IS NULL;
```

- [ ] **Step 2: Viết kiểm thử ràng buộc**

```java
package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
class GuardianLinkMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID insertParent(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO parent_profiles
                    (id, user_id, full_name, enabled, version, created_at, updated_at)
                VALUES (?, NULL, ?, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, name);
        return id;
    }

    private UUID anyStudentId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM students LIMIT 1", UUID.class);
    }

    @Test
    void allowsManyTeacherProfilesWithoutAUserAccount() {
        for (int i = 0; i < 2; i++) {
            jdbcTemplate.update(
                    """
                    INSERT INTO teacher_profiles
                        (id, user_id, teacher_code, full_name, enabled, version,
                         created_at, updated_at)
                    VALUES (?, NULL, ?, ?, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    UUID.randomUUID(), "GV" + i, "Giáo viên " + i);
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM teacher_profiles WHERE user_id IS NULL", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void rejectsASecondActiveLinkForTheSameParentAndStudent() {
        UUID parentId = insertParent("Nguyễn Văn A");
        UUID studentId = anyStudentId();
        jdbcTemplate.update(
                """
                INSERT INTO parent_student_links
                    (id, parent_id, student_id, relationship, contact_order,
                     effective_from, effective_to, created_at, updated_at)
                VALUES (?, ?, ?, 'FATHER', 1, CURRENT_DATE, NULL,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), parentId, studentId);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO parent_student_links
                            (id, parent_id, student_id, relationship, contact_order,
                             effective_from, effective_to, created_at, updated_at)
                        VALUES (?, ?, ?, 'GUARDIAN', 2, CURRENT_DATE, NULL,
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), parentId, studentId))
                .hasMessageContaining("uk_parent_student_links_active");
    }

    @Test
    void rejectsAnUnknownRelationship() {
        UUID parentId = insertParent("Trần Thị B");
        UUID studentId = anyStudentId();
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO parent_student_links
                            (id, parent_id, student_id, relationship, contact_order,
                             effective_from, effective_to, created_at, updated_at)
                        VALUES (?, ?, ?, 'UNCLE', 1, CURRENT_DATE, NULL,
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), parentId, studentId))
                .hasMessageContaining("ck_parent_student_links_relationship");
    }
}
```

- [ ] **Step 3: Chạy để xác nhận đỏ, rồi thêm migration và chạy lại**

Run: `cd backend && ./mvnw test -Dtest=GuardianLinkMigrationIntegrationTest`
Expected: đỏ trước (`relation "teacher_profiles" does not exist`), xanh sau, 3 test.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V20__create_teacher_parent_profiles.sql \
        backend/src/test/java/vn/edu/fpt/myschool/auth/GuardianLinkMigrationIntegrationTest.java
git commit -m "feat(auth): hồ sơ giáo viên, phụ huynh và liên kết giám hộ"
```

---

## Task 7: Chuyển API học sinh sang `/api/v1/student/**`

**Files:**
- Modify: mọi `@RequestMapping` của controller học sinh trong `home`, `timetable`, `grades`, `events`, `forms`, `clubs`, `assistant`
- Modify: `backend/.../auth/infrastructure/security/AuthSecurityConfiguration.java`
- Modify: `lib/src/core/network/` — base path phía Flutter
- Modify: `e2e/**` — 11 bộ Playwright

Đây là bước thuần cơ học, rủi ro cao nhất R1 (spec mục 6 và 12). Làm một mình một commit, chạy full hồi quy ngay sau.

- [ ] **Step 1: Liệt kê toàn bộ đường dẫn hiện có**

Run:
```bash
grep -rn "RequestMapping\|GetMapping\|PostMapping" backend/src/main/java --include="*.java" \
  | grep -E "/api/v1/(home|timetable|grades|events|forms|clubs|assistant)"
```
Ghi lại danh sách trước khi sửa để đối chiếu sau.

- [ ] **Step 2: Sửa từng controller**

Ví dụ `HomeController`: `@RequestMapping("/api/v1/home")` → `@RequestMapping("/api/v1/student/home")`.
Áp dụng tương tự: `/timetable` → `/student/timetable`, `/grades` → `/student/grades`, `/events` → `/student/events`, `/forms` → `/student/forms`, `/clubs` → `/student/clubs`, `/assistant` → `/student/assistant`.

- [ ] **Step 3: Sửa `AuthSecurityConfiguration`**

```java
.requestMatchers("/api/v1/student", "/api/v1/student/**")
    .hasRole(UserRole.STUDENT.name())
.requestMatchers("/api/v1/teacher", "/api/v1/teacher/**")
    .hasRole(UserRole.TEACHER.name())
.requestMatchers("/api/v1/parent", "/api/v1/parent/**")
    .hasRole(UserRole.PARENT.name())
.requestMatchers("/api/v1/admin", "/api/v1/admin/**")
    .hasRole(UserRole.ADMIN.name())
.anyRequest().denyAll());
```

Bảy dòng `requestMatchers` cũ liệt kê từng đường dẫn học sinh bị thay bằng một dòng. `denyAll()` mặc định giữ nguyên.

- [ ] **Step 4: Sửa phía Flutter**

Tìm nơi khai báo đường dẫn:
```bash
grep -rn "api/v1" lib/ --include="*.dart"
```
Đổi tương ứng.

- [ ] **Step 5: Sửa E2E**

```bash
grep -rln "api/v1/\(home\|timetable\|grades\|events\|forms\|clubs\|assistant\)" e2e/
```

- [ ] **Step 6: Chạy full hồi quy — cổng nghiệm thu của task này**

Run: `cd backend && ./mvnw test`
Expected: PASS toàn bộ

Run: `flutter test`
Expected: PASS toàn bộ

Run: `npx playwright test`
Expected: PASS 11 bộ

Nếu bất kỳ bộ nào đỏ, sửa cho xanh trước khi commit. Không commit khi còn đỏ.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(api): chuyển API học sinh sang /api/v1/student/**"
```

---

## Task 8: Kiểm thử phân quyền phủ định

**Files:**
- Create: `backend/src/test/java/vn/edu/fpt/myschool/auth/RoleIsolationIntegrationTest.java`

Cổng nghiệm thu R1 (spec mục 13): giáo viên và phụ huynh không gọi được API của nhau. Kiểm thử phải chứng minh, không chỉ mô tả (spec mục 2.1).

- [ ] **Step 1: Viết kiểm thử**

```java
package vn.edu.fpt.myschool.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class RoleIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    // tokenFor(...) dựng access token cho một vai bất kỳ để kiểm thử tầng
    // security tách biệt khỏi luồng đăng nhập.
    private String tokenFor(String role) {
        // Dùng cùng JwtEncoder của ứng dụng nên token hợp lệ với JwtDecoder.
        // Chi tiết dựng claim giống JwtAccessTokenIssuer: iss, sub, role.
        throw new UnsupportedOperationException(
                "Điền theo AuthProperties.issuer() và UUID người dùng thật khi cài đặt");
    }

    @Test
    void teacherTokenCannotReachStudentNamespace() throws Exception {
        mockMvc.perform(get("/api/v1/student/home")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void parentTokenCannotReachTeacherNamespace() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("PARENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentTokenCannotReachAdminNamespace() throws Exception {
        mockMvc.perform(get("/api/v1/admin/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownNamespaceIsDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/principal/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
```

`tokenFor` phải được cài đặt thật khi thực thi task — dựng claim `iss` từ `AuthProperties.issuer()`, `sub` là UUID người dùng seed, `role` là tham số. Không để `UnsupportedOperationException` trong bản commit.

- [ ] **Step 2: Chạy**

Run: `cd backend && ./mvnw test -Dtest=RoleIsolationIntegrationTest`
Expected: PASS, 4 test

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/vn/edu/fpt/myschool/auth/RoleIsolationIntegrationTest.java
git commit -m "test(auth): kiểm thử phân quyền phủ định giữa bốn vai"
```

---

## Task 9-11: Admin Web — Giáo viên, Phụ huynh, Liên kết

Ba trang admin theo spec mục 9. Mỗi trang là một task độc lập, cùng khuôn: endpoint `/api/v1/admin/...`, TanStack Query hook, trang MUI, Vitest, Playwright.

Chi tiết đầy đủ được viết khi tới Task 9 — phụ thuộc vào khuôn `StudentsPage.tsx` hiện có, phải đọc trước để theo đúng khuôn thay vì đoán.

---

## Task 12: Xoá `users.role`

**Files:**
- Create: `backend/src/main/resources/db/migration/V21__drop_users_role.sql`
- Modify: `backend/.../auth/infrastructure/persistence/UserJpaEntity.java` — bỏ field `role`

Chỉ chạy khi Task 4 đã xong và không còn code nào đọc `users.role`.

- [ ] **Step 1: Xác nhận không còn tham chiếu**

Run:
```bash
grep -rn "users.role\|getRole()\|\"role\"" backend/src/main/java --include="*.java" | grep -v user_roles
```
Expected: chỉ còn `UserRoleJpaEntity` và `JwtAccessTokenIssuer` (claim JWT), không còn `UserJpaEntity.role`.

- [ ] **Step 2: Viết migration**

```sql
-- V21__drop_users_role.sql
-- user_roles là nguồn sự thật duy nhất về vai kể từ V19. Giữ users.role tạo ra
-- nguồn thứ hai và đó là nguồn sinh lỗi phân quyền (spec mục 5.1).
ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users DROP COLUMN role;
```

- [ ] **Step 3: Bỏ field khỏi `UserJpaEntity`**

Xoá `@Enumerated`/`@Column(name = "role")`/`private UserRole role`, getter `getRole()`, và tham số `role` khỏi constructor. Sửa `AuthDataSeeder` và `AdminDataSeeder` ghi vào `user_roles` thay vì truyền `role` vào constructor.

- [ ] **Step 4: Chạy full hồi quy**

Run: `cd backend && ./mvnw test`
Expected: PASS toàn bộ

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(auth): xoá users.role, user_roles là nguồn sự thật duy nhất"
```

---

## Cổng nghiệm thu R1

Theo spec mục 13, R1 chỉ hoàn thành khi:

- [ ] `cd backend && ./mvnw test` xanh toàn bộ
- [ ] `flutter test` xanh toàn bộ
- [ ] `npx playwright test` xanh 11 bộ học sinh + bộ admin
- [ ] `RoleIsolationIntegrationTest` chứng minh giáo viên và phụ huynh không gọi được API của nhau
- [ ] `users.role` không còn tồn tại; `grep -rn "users.role" backend/src` không ra kết quả
- [ ] `docs/MOBILE_ROLES_ROADMAP.md` đã xoá hoặc đánh dấu đã thay thế (spec mục 1.1)
