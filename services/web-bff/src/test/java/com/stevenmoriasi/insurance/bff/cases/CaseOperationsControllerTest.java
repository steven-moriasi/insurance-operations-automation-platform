package com.stevenmoriasi.insurance.bff.cases;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = "spring.security.oauth2.client.registration.keycloak.client-secret=test-only")
@AutoConfigureMockMvc
class CaseOperationsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CaseServiceClient caseService;
    @MockitoBean private UserAccessTokenProvider tokens;

    @Test
    void forwardsAuthorizedCaseQueriesWithTheDelegatedToken() throws Exception {
        when(tokens.currentToken(org.mockito.ArgumentMatchers.any())).thenReturn("access-token");
        when(caseService.exchange(
                        HttpMethod.GET, "/internal/api/v1/claims/mine", null, "access-token"))
                .thenReturn(
                        ResponseEntity.ok(
                                objectMapper.readTree(
                                        """
                                        [{"claimReference":"CLM-1001","status":"OPEN"}]
                                        """)));

        mockMvc.perform(get("/api/cases/mine").with(user("claims.agent").roles("CLAIMS_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimReference").value("CLM-1001"));

        verify(caseService)
                .exchange(HttpMethod.GET, "/internal/api/v1/claims/mine", null, "access-token");
    }

    @Test
    void rejectsAuthenticatedUsersWithoutACaseRole() throws Exception {
        mockMvc.perform(get("/api/cases/mine").with(user("broker.user").roles("BROKER")))
                .andExpect(status().isForbidden());
    }
}
