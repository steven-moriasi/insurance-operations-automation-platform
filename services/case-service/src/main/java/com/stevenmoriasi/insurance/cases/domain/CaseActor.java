package com.stevenmoriasi.insurance.cases.domain;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record CaseActor(String username, Set<String> roles) {

    public CaseActor {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        roles =
                roles == null
                        ? Set.of()
                        : roles.stream()
                                .map(CaseActor::normalizeRole)
                                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasAnyRole(String... expectedRoles) {
        for (String role : expectedRoles) {
            if (roles.contains(normalizeRole(role))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeRole(String role) {
        String normalized = role.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
