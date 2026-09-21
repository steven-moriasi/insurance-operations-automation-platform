package com.stevenmoriasi.insurance.cases.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class JwtRoleMapper implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId;
    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    public JwtRoleMapper(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopes.convert(jwt));
        addRoles(authorities, nestedRoles(jwt.getClaim("realm_access")));
        Object resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> resources) {
            addRoles(authorities, nestedRoles(resources.get(clientId)));
        }
        return authorities;
    }

    private static List<?> nestedRoles(Object claim) {
        if (claim instanceof Map<?, ?> values && values.get("roles") instanceof List<?> roles) {
            return roles;
        }
        return List.of();
    }

    private static void addRoles(Set<GrantedAuthority> authorities, List<?> roles) {
        roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(JwtRoleMapper::toAuthority)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }

    private static String toAuthority(String role) {
        String normalized = role.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
