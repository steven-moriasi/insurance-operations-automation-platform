package com.stevenmoriasi.insurance.workflows.claims;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stevenmoriasi.insurance.workflows.claims.MotorClaimWorkflowService.WorkflowReference;
import com.stevenmoriasi.insurance.workflows.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = MotorClaimWorkflowController.class,
        properties = "insurance.security.client-id=insurance-workflow-service")
@Import(SecurityConfig.class)
class MotorClaimWorkflowControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MotorClaimWorkflowService workflows;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void letsClaimsOfficersStartMotorClaimWorkflows() throws Exception {
        when(workflows.start(any()))
                .thenReturn(new WorkflowReference("motor-claim-CLM-4001", "run-1"));

        mockMvc.perform(
                        post("/internal/api/v1/workflows/claims")
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CLAIMS_OFFICER")))
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "claimReference": "CLM-4001",
                                          "evidenceSlaSeconds": 3600,
                                          "assessmentSlaSeconds": 3600,
                                          "decisionSlaSeconds": 3600,
                                          "settlementSlaSeconds": 3600
                                        }
                                        """))
                .andExpect(status().isAccepted());

        verify(workflows).start(any());
    }

    @Test
    void rejectsWorkflowStartsFromUsersWithoutAClaimsRole() throws Exception {
        mockMvc.perform(
                        post("/internal/api/v1/workflows/claims")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BROKER")))
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "claimReference": "CLM-4002",
                                          "evidenceSlaSeconds": 3600,
                                          "assessmentSlaSeconds": 3600,
                                          "decisionSlaSeconds": 3600,
                                          "settlementSlaSeconds": 3600
                                        }
                                        """))
                .andExpect(status().isForbidden());
    }
}
