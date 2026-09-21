package com.stevenmoriasi.insurance.cases.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRoleMapperTest {

    @Test
    void mapsRealmAndClientRolesToNormalizedAuthorities() {
        Jwt jwt =
                new Jwt(
                        "token",
                        Instant.parse("2026-09-21T08:00:00Z"),
                        Instant.parse("2026-09-21T09:00:00Z"),
                        Map.of("alg", "none"),
                        Map.of(
                                "sub",
                                "actor-1",
                                "scope",
                                "openid",
                                "realm_access",
                                Map.of("roles", List.of("claims-officer")),
                                "resource_access",
                                Map.of(
                                        "insurance-case-service",
                                        Map.of("roles", List.of("senior_claims_approver")))));

        assertThat(new JwtRoleMapper("insurance-case-service").convert(jwt))
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder(
                        "SCOPE_openid", "ROLE_CLAIMS_OFFICER", "ROLE_SENIOR_CLAIMS_APPROVER");
    }
}
