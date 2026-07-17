package vn.edu.fpt.myschool.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.fpt.myschool.auth.application.AuthProperties;
import vn.edu.fpt.myschool.auth.domain.UserRole;

/**
 * Proves that one role cannot reach another role's namespace.
 *
 * <p>Tokens are minted directly rather than obtained by logging in, so that the filter chain is
 * exercised on its own. That is deliberate: this asserts what the security layer denies, not what
 * the login flow happens to hand out today.
 */
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

    @Autowired
    private AuthProperties authProperties;

    private String tokenFor(UserRole role) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.issuer())
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(10, ChronoUnit.MINUTES))
                .id(UUID.randomUUID().toString())
                .claim("role", role.name())
                .build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private void expectForbidden(String path, UserRole role) throws Exception {
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotReachTheParentNamespace() throws Exception {
        expectForbidden("/api/v1/parent/children", UserRole.TEACHER);
    }

    @Test
    void parentCannotReachTheTeacherNamespace() throws Exception {
        expectForbidden("/api/v1/teacher/classes", UserRole.PARENT);
        expectForbidden("/api/v1/teacher/schedule", UserRole.PARENT);
    }

    @Test
    void neitherTeacherNorParentCanReachTheStudentNamespace() throws Exception {
        expectForbidden("/api/v1/home", UserRole.TEACHER);
        expectForbidden("/api/v1/grades", UserRole.PARENT);
        expectForbidden("/api/v1/timetable", UserRole.TEACHER);
        expectForbidden("/api/v1/forms", UserRole.PARENT);
    }

    @Test
    void studentCannotReachAnyOtherNamespace() throws Exception {
        expectForbidden("/api/v1/teacher/classes", UserRole.STUDENT);
        expectForbidden("/api/v1/parent/children", UserRole.STUDENT);
        expectForbidden("/api/v1/admin/students", UserRole.STUDENT);
    }

    @Test
    void adminCannotReachTheStudentTeacherOrParentNamespace() throws Exception {
        expectForbidden("/api/v1/home", UserRole.ADMIN);
        expectForbidden("/api/v1/teacher/classes", UserRole.ADMIN);
        expectForbidden("/api/v1/parent/children", UserRole.ADMIN);
    }

    @Test
    void anUnmappedNamespaceIsDeniedForEveryRole() throws Exception {
        for (UserRole role : UserRole.values()) {
            expectForbidden("/api/v1/principal/dashboard", role);
        }
    }

    /**
     * Distinguishes the namespace rule from the default deny.
     *
     * <p>Without the teacher and parent rules every request to those paths would fall through to
     * {@code denyAll()} and answer 403 — which would make the tests above pass for the wrong
     * reason. A role that owns the namespace must get past authorisation and fail later, on the
     * missing handler, so 404 here is the evidence that the rule itself is doing the work.
     *
     * <p>Deliberately aimed at paths with no handler. Once a namespace grows real endpoints the
     * trick stops working there, and the stronger proof moves to those endpoints' own tests,
     * where a wrong role gets 403 and the right one gets data.
     */
    @Test
    void aRoleReachesItsOwnNamespaceAndOnlyMissesTheEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/no-such-endpoint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(UserRole.TEACHER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/parent/no-such-endpoint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(UserRole.PARENT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aRequestWithoutATokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/classes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/parent/children")).andExpect(status().isUnauthorized());
    }
}
