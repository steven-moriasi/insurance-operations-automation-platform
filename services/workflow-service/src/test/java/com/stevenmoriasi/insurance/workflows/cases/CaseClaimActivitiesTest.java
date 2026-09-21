package com.stevenmoriasi.insurance.workflows.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.failure.ApplicationFailure;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CaseClaimActivitiesTest {

    private final CaseServiceGateway cases = mock(CaseServiceGateway.class);
    private final CaseClaimActivities activities = new CaseClaimActivities(cases);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsAnActivePolicyCoveringTheCurrentDate() throws Exception {
        JsonNode caseView =
                objectMapper.readTree(
                        """
                        {
                          "policy": {
                            "status": "ACTIVE",
                            "coverStartDate": "2020-01-01",
                            "coverEndDate": "2030-12-31"
                          }
                        }
                        """);
        when(cases.getCase("CLM-3001")).thenReturn(caseView);

        activities.verifyCoverage("CLM-3001");

        verify(cases).getCase("CLM-3001");
    }

    @Test
    void treatsInactiveCoverageAsANonRetryableBusinessFailure() throws Exception {
        JsonNode caseView =
                objectMapper.readTree(
                        """
                        {
                          "policy": {
                            "status": "LAPSED",
                            "coverStartDate": "2020-01-01",
                            "coverEndDate": "2030-12-31"
                          }
                        }
                        """);
        when(cases.getCase("CLM-3002")).thenReturn(caseView);

        assertThatThrownBy(() -> activities.verifyCoverage("CLM-3002"))
                .isInstanceOfSatisfying(
                        ApplicationFailure.class,
                        failure -> {
                            assertThat(failure.isNonRetryable()).isTrue();
                            assertThat(failure.getType()).isEqualTo("BUSINESS_RULE_FAILURE");
                        });
    }

    @Test
    void delegatesTaskLifecycleToTheCaseService() {
        UUID taskId = UUID.randomUUID();
        when(cases.createTask(
                        "CLM-3003",
                        "EVIDENCE_COLLECTION",
                        "CLAIMS_OFFICER",
                        Instant.ofEpochMilli(2000)))
                .thenReturn(taskId);

        String created =
                activities.createTask("CLM-3003", "EVIDENCE_COLLECTION", "CLAIMS_OFFICER", 2000);
        activities.cancelTask("CLM-3003", created, "Claim withdrawn");
        activities.recordCompletion("CLM-3003");

        assertThat(created).isEqualTo(taskId.toString());
        verify(cases).cancelTask("CLM-3003", taskId, "Claim withdrawn");
        verify(cases).recordCompletion("CLM-3003");
    }
}
