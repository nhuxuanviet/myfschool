package vn.edu.fpt.myschool.auth.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.application.AuthProperties;
import vn.edu.fpt.myschool.auth.application.port.AccessTokenIssuer;
import vn.edu.fpt.myschool.auth.domain.UserAccount;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;
    private final Clock clock;

    public JwtAccessTokenIssuer(
            JwtEncoder jwtEncoder,
            AuthProperties properties,
            Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issue(UserAccount userAccount) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userAccount.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", userAccount.role().name());
        switch (userAccount.role()) {
            case STUDENT -> claims.claim(
                    "studentId", userAccount.student().id().toString());
            case ADMIN -> claims.claim(
                    "adminId", userAccount.admin().id().toString());
        }
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(headers, claims.build()))
                .getTokenValue();
        return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
    }
}
