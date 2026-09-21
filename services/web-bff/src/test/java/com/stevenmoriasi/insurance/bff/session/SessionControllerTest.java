package com.stevenmoriasi.insurance.bff.session;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = "spring.security.oauth2.client.registration.keycloak.client-secret=test-only")
@AutoConfigureMockMvc
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void apiReturnsUnauthorizedInsteadOfRedirecting() throws Exception {
        mockMvc.perform(get("/api/session")).andExpect(status().isUnauthorized());
    }

    @Test
    void exposesAuthenticatedSessionAndNormalizedRoles() throws Exception {
        mockMvc.perform(
                        get("/api/session")
                                .with(
                                        user("claims.agent")
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CLAIMS_OFFICER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("claims.agent"))
                .andExpect(jsonPath("$.roles", contains("CLAIMS_OFFICER")))
                .andExpect(jsonPath("$.csrfToken").isNotEmpty());
    }

    @Test
    void filtersWorkspacesByServerSideRole() throws Exception {
        mockMvc.perform(
                        get("/api/workspaces")
                                .with(user("finance.operator").roles("FINANCE_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("finance"));
    }

    @Test
    void adminDiagnosticsRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(
                        get("/api/admin/diagnostics")
                                .with(user("claims.agent").roles("CLAIMS_OFFICER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/session/logout").with(user("claims.agent")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/session/logout").with(user("claims.agent")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
