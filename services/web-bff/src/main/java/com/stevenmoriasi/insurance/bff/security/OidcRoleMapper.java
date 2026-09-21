package com.stevenmoriasi.insurance.bff.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

public final class OidcRoleMapper {

    private final String clientId;

    public OidcRoleMapper(String clientId) {
        this.clientId = clientId;
    }

    public Collection<? extends GrantedAuthority> mapAuthorities(
            Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new HashSet<>(authorities);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidcAuthority) {
                addClaimRoles(mapped, oidcAuthority.getAttributes());
            } else if (authority instanceof OAuth2UserAuthority oauthAuthority) {
                addClaimRoles(mapped, oauthAuthority.getAttributes());
            }
        }
        return Set.copyOf(mapped);
    }

    private void addClaimRoles(Set<GrantedAuthority> mapped, Map<String, Object> attributes) {
        addRoles(mapped, nestedRoles(attributes.get("realm_access")));
        Object resourceAccess = attributes.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            addRoles(mapped, nestedRoles(resources.get(clientId)));
        }
    }

    private static Collection<?> nestedRoles(Object claim) {
        if (claim instanceof Map<?, ?> values
                && values.get("roles") instanceof Collection<?> roles) {
            return roles;
        }
        return Set.of();
    }

    private static void addRoles(Set<GrantedAuthority> mapped, Collection<?> roles) {
        roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(OidcRoleMapper::toAuthority)
                .map(SimpleGrantedAuthority::new)
                .forEach(mapped::add);
    }

    private static String toAuthority(String role) {
        String normalized = role.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
