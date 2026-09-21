package com.stevenmoriasi.insurance.bff.session;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SessionController {

    @GetMapping("/public/config")
    public PublicConfig publicConfig() {
        return new PublicConfig(
                "Insurance Operations Automation Platform", "/oauth2/authorization/keycloak");
    }

    @GetMapping("/session")
    public SessionView session(Authentication authentication, CsrfToken csrfToken) {
        String displayName =
                authentication.getPrincipal() instanceof OidcUser oidcUser
                        ? oidcUser.getFullName()
                        : authentication.getName();
        List<String> roles =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring("ROLE_".length()))
                        .sorted()
                        .toList();
        return new SessionView(authentication.getName(), displayName, roles, csrfToken.getToken());
    }

    @GetMapping("/dashboard")
    public DashboardView dashboard(Authentication authentication) {
        boolean processOwner = hasRole(authentication, "PROCESS_OWNER");
        return new DashboardView(
                new Metric("Open cases", processOwner ? 42 : 8, "Across permitted work queues"),
                new Metric("Awaiting evidence", processOwner ? 11 : 3, "Customer action required"),
                new Metric("SLA risks", processOwner ? 5 : 1, "Due within four hours"),
                List.of(
                        new Activity(
                                "CLM-1042", "Motor claim referred for assessor review", "12 min"),
                        new Activity("CLM-1038", "Evidence checklist completed", "31 min"),
                        new Activity("REN-0204", "Renewal offer accepted", "1 hr")));
    }

    @GetMapping("/workspaces")
    public List<Workspace> workspaces(Authentication authentication) {
        List<String> authorities =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();
        return Workspace.catalogue().stream()
                .filter(workspace -> workspace.isVisibleTo(authorities))
                .toList();
    }

    @GetMapping("/admin/diagnostics")
    public Map<String, String> diagnostics() {
        return Map.of("status", "available", "scope", "synthetic-reference");
    }

    private static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    public record PublicConfig(String applicationName, String loginUrl) {}

    public record SessionView(
            String username, String displayName, List<String> roles, String csrfToken) {}

    public record DashboardView(
            Metric openCases,
            Metric awaitingEvidence,
            Metric slaRisks,
            List<Activity> recentActivity) {}

    public record Metric(String label, int value, String detail) {}

    public record Activity(String caseReference, String summary, String age) {}

    public record Workspace(
            String id, String label, String description, String path, List<String> roles) {

        static List<Workspace> catalogue() {
            return List.of(
                    new Workspace(
                            "claims",
                            "Claims workspace",
                            "Triage, evidence, assessment, and decisions",
                            "/claims",
                            List.of(
                                    "ROLE_CONTACT_CENTRE_AGENT",
                                    "ROLE_CLAIMS_OFFICER",
                                    "ROLE_ASSESSOR",
                                    "ROLE_FRAUD_REVIEWER",
                                    "ROLE_APPROVER")),
                    new Workspace(
                            "finance",
                            "Settlement operations",
                            "Release and reconcile approved settlements",
                            "/finance",
                            List.of("ROLE_FINANCE_OPERATOR", "ROLE_APPROVER")),
                    new Workspace(
                            "process",
                            "Process intelligence",
                            "Inspect service levels, exceptions, and automation outcomes",
                            "/process",
                            List.of("ROLE_PROCESS_OWNER", "ROLE_PLATFORM_ADMIN")),
                    new Workspace(
                            "administration",
                            "Platform administration",
                            "Manage integrations, reference data, and access",
                            "/administration",
                            List.of("ROLE_PLATFORM_ADMIN")));
        }

        boolean isVisibleTo(List<String> authorities) {
            return roles.stream().anyMatch(authorities::contains);
        }
    }
}
