package com.stevenmoriasi.insurance.cases.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void requiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/internal/api/v1/claims/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void reportsAndReturnsAnOwnedClaim() throws Exception {
        mockMvc.perform(
                        post("/internal/api/v1/claims")
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.claim(
                                                                        "preferred_username",
                                                                        "claims.agent"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CLAIMS_OFFICER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reportClaimJson("CLM-API-1001")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/internal/api/v1/claims/CLM-API-1001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.ownerUsername").value("claims.agent"));

        mockMvc.perform(
                        get("/internal/api/v1/claims/CLM-API-1001")
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.claim(
                                                                        "preferred_username",
                                                                        "claims.agent"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CLAIMS_OFFICER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy.policyNumber").value("POL-CLM-API-1001"))
                .andExpect(jsonPath("$.claimant.fullName").value("Amina Wanjiku"))
                .andExpect(jsonPath("$.auditEvents[0].eventType").value("CLAIM_REPORTED"));
    }

    @Test
    void hidesAnOwnedClaimFromAnotherOfficer() throws Exception {
        mockMvc.perform(
                        post("/internal/api/v1/claims")
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.claim(
                                                                        "preferred_username",
                                                                        "claims.owner"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CLAIMS_OFFICER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reportClaimJson("CLM-API-1002")))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/internal/api/v1/claims/CLM-API-1002")
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.claim(
                                                                        "preferred_username",
                                                                        "claims.other"))
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_CLAIMS_OFFICER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:insurance:case-error:FORBIDDEN"));
    }

    private static String reportClaimJson(String claimReference) {
        return """
        {
          "claimReference": "%s",
          "policyNumber": "POL-%s",
          "productCode": "PRIVATE_MOTOR",
          "policyStatus": "ACTIVE",
          "coverStartDate": "2026-01-01",
          "coverEndDate": "2026-12-31",
          "currency": "KES",
          "claimantExternalReference": "PTY-%s",
          "claimantName": "Amina Wanjiku",
          "claimantPhoneNumber": "+254700000001",
          "claimantEmailAddress": "amina@example.test",
          "lossDate": "2026-09-20"
        }
        """
                .formatted(claimReference, claimReference, claimReference);
    }
}
