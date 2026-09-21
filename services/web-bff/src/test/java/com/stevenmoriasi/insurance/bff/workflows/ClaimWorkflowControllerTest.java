package com.stevenmoriasi.insurance.bff.workflows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stevenmoriasi.insurance.bff.cases.UserAccessTokenProvider;
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
class ClaimWorkflowControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private WorkflowServiceClient workflowService;
    @MockitoBean private UserAccessTokenProvider tokens;

    @Test
    void forwardsWorkflowQueriesWithTheDelegatedUserToken() throws Exception {
        when(tokens.currentToken(org.mockito.ArgumentMatchers.any())).thenReturn("access-token");
        when(workflowService.exchange(
                        HttpMethod.GET,
                        "/internal/api/v1/workflows/claims/CLM-4001",
                        null,
                        "access-token"))
                .thenReturn(
                        ResponseEntity.ok(
                                objectMapper.readTree(
                                        """
                                        {"stage":"AWAITING_EVIDENCE","overdueStages":[]}
                                        """)));

        mockMvc.perform(
                        get("/api/workflows/claims/CLM-4001")
                                .with(user("claims.agent").roles("CLAIMS_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("AWAITING_EVIDENCE"));

        verify(workflowService)
                .exchange(
                        HttpMethod.GET,
                        "/internal/api/v1/workflows/claims/CLM-4001",
                        null,
                        "access-token");
    }
}
