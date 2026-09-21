package com.stevenmoriasi.insurance.bff.integrations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stevenmoriasi.insurance.bff.cases.UserAccessTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = "spring.security.oauth2.client.registration.keycloak.client-secret=test-only")
@AutoConfigureMockMvc
class InsuranceIntegrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private IntegrationServiceClient integrations;
    @MockitoBean private UserAccessTokenProvider tokens;

    @Test
    void forwardsPaymentInstructionsWithTheUserTokenAndIdempotencyKey() throws Exception {
        when(tokens.currentToken(any())).thenReturn("access-token");
        when(integrations.exchange(
                        eq(HttpMethod.POST),
                        eq("/internal/api/v1/integrations/payments"),
                        any(JsonNode.class),
                        eq("access-token"),
                        eq("payment-CLM-4001")))
                .thenReturn(
                        ResponseEntity.ok(
                                objectMapper.readTree(
                                        """
                                        {"status":"INSTRUCTED","currency":"KES"}
                                        """)));

        mockMvc.perform(
                        post("/api/integrations/payments")
                                .with(user("finance.operator").roles("FINANCE_OPERATOR"))
                                .with(csrf())
                                .header("Idempotency-Key", "payment-CLM-4001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"claimReference":"CLM-4001","amount":12500,"currency":"KES"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INSTRUCTED"));

        verify(integrations)
                .exchange(
                        eq(HttpMethod.POST),
                        eq("/internal/api/v1/integrations/payments"),
                        any(JsonNode.class),
                        eq("access-token"),
                        eq("payment-CLM-4001"));
    }
}
