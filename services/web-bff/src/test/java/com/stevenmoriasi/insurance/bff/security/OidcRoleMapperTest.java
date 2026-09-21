package com.stevenmoriasi.insurance.bff.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

class OidcRoleMapperTest {

    @Test
    void mapsRealmAndClientRolesToSpringAuthorities() {
        OidcIdToken token =
                OidcIdToken.withTokenValue("token")
                        .subject("user-1")
                        .claim("realm_access", Map.of("roles", List.of("claims-officer")))
                        .claim(
                                "resource_access",
                                Map.of(
                                        "insurance-operations-web",
                                        Map.of("roles", List.of("process-owner"))))
                        .build();
        OidcUserAuthority oidcAuthority = new OidcUserAuthority(token);

        Set<String> mapped =
                new OidcRoleMapper("insurance-operations-web")
                        .mapAuthorities(List.of(oidcAuthority)).stream()
                                .map(authority -> authority.getAuthority())
                                .collect(java.util.stream.Collectors.toSet());

        assertThat(mapped)
                .contains(
                        "ROLE_CLAIMS_OFFICER", "ROLE_PROCESS_OWNER", oidcAuthority.getAuthority());
    }

    @Test
    void preservesExistingAuthorities() {
        SimpleGrantedAuthority existing = new SimpleGrantedAuthority("SCOPE_openid");

        assertThat(new OidcRoleMapper("web").mapAuthorities(List.of(existing)))
                .extracting(authority -> authority.getAuthority())
                .contains("SCOPE_openid");
    }
}
