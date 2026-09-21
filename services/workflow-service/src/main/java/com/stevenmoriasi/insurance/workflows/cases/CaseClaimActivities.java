package com.stevenmoriasi.insurance.workflows.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.stevenmoriasi.insurance.workflows.claims.ClaimActivities;
import io.temporal.failure.ApplicationFailure;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CaseClaimActivities implements ClaimActivities {

    private static final String BUSINESS_RULE_FAILURE = "BUSINESS_RULE_FAILURE";

    private final CaseServiceGateway cases;
    private final Clock clock;

    public CaseClaimActivities(CaseServiceGateway cases) {
        this.cases = cases;
        this.clock = Clock.systemUTC();
    }

    @Override
    public void verifyCoverage(String claimReference) {
        try {
            JsonNode caseView = cases.getCase(claimReference);
            JsonNode policy = caseView.path("policy");
            LocalDate today = LocalDate.now(clock);
            LocalDate coverStart = LocalDate.parse(policy.path("coverStartDate").asText());
            LocalDate coverEnd = LocalDate.parse(policy.path("coverEndDate").asText());
            if (!"ACTIVE".equals(policy.path("status").asText())
                    || today.isBefore(coverStart)
                    || today.isAfter(coverEnd)) {
                throw businessFailure("Policy coverage is not active for this claim");
            }
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw businessFailure("Claim or policy context cannot be verified");
            }
            throw exception;
        }
    }

    @Override
    public String createTask(
            String claimReference, String taskType, String assigneeRole, long dueAtEpochMillis) {
        return cases.createTask(
                        claimReference,
                        taskType,
                        assigneeRole,
                        Instant.ofEpochMilli(dueAtEpochMillis))
                .toString();
    }

    @Override
    public void escalate(String claimReference, String stage, String taskId) {
        cases.createTask(
                claimReference,
                stage + "_ESCALATION",
                "CLAIMS_SUPERVISOR",
                clock.instant().plusSeconds(4 * 60 * 60));
    }

    @Override
    public void cancelTask(String claimReference, String taskId, String reason) {
        cases.cancelTask(claimReference, UUID.fromString(taskId), reason);
    }

    @Override
    public void recordCompletion(String claimReference) {
        cases.recordCompletion(claimReference);
    }

    private static ApplicationFailure businessFailure(String message) {
        return ApplicationFailure.newNonRetryableFailure(message, BUSINESS_RULE_FAILURE);
    }
}
