package com.stevenmoriasi.insurance.integrations.legacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stevenmoriasi.insurance.integrations.domain.IntegrationAttempt;
import com.stevenmoriasi.insurance.integrations.domain.IntegrationAttemptRepository;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEvent;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEventRepository;
import com.stevenmoriasi.insurance.integrations.legacy.LegacyPolicyGateway.LegacyPolicy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyVerificationService {

    private static final String OPERATION_TYPE = "POLICY_COVERAGE_VERIFICATION";

    private final LegacyPolicyGateway legacyPolicies;
    private final IntegrationAttemptRepository attempts;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PolicyVerificationService(
            LegacyPolicyGateway legacyPolicies,
            IntegrationAttemptRepository attempts,
            OutboxEventRepository outbox,
            ObjectMapper objectMapper) {
        this.legacyPolicies = legacyPolicies;
        this.attempts = attempts;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public PolicyVerification verify(
            String idempotencyKey, String policyNumber, LocalDate lossDate) {
        String requestHash = sha256(policyNumber + "|" + lossDate);
        IntegrationAttempt existing = attempts.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IntegrationConflictException(
                        "Idempotency key was already used for a different request");
            }
            if (existing.getStatus() == IntegrationAttempt.IntegrationStatus.COMPLETED) {
                return read(existing.getResponseJson());
            }
            throw new IntegrationConflictException("The integration request is still pending");
        }

        Instant now = clock.instant();
        IntegrationAttempt attempt =
                attempts.save(
                        new IntegrationAttempt(
                                UUID.randomUUID(),
                                idempotencyKey,
                                OPERATION_TYPE,
                                policyNumber,
                                requestHash,
                                now));
        LegacyPolicy policy =
                legacyPolicies
                        .findByPolicyNumber(policyNumber)
                        .orElseThrow(
                                () ->
                                        new IntegrationNotFoundException(
                                                "Synthetic legacy policy was not found"));
        boolean covered =
                "ACTIVE".equals(policy.status())
                        && !lossDate.isBefore(policy.coverStartDate())
                        && !lossDate.isAfter(policy.coverEndDate());
        PolicyVerification response =
                new PolicyVerification(
                        attempt.getId(),
                        policy.policyNumber(),
                        policy.productCode(),
                        policy.status(),
                        policy.coverStartDate(),
                        policy.coverEndDate(),
                        policy.currency(),
                        lossDate,
                        covered);
        String responseJson = write(response);
        attempt.complete(responseJson, clock.instant());
        outbox.save(
                new OutboxEvent(
                        UUID.randomUUID(),
                        "POLICY",
                        policy.policyNumber(),
                        "insurance.policy.coverage-verified",
                        responseJson,
                        clock.instant()));
        return response;
    }

    private PolicyVerification read(String responseJson) {
        try {
            return objectMapper.readValue(responseJson, PolicyVerification.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored integration response is invalid", exception);
        }
    }

    private String write(PolicyVerification response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Integration response cannot be serialized", exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record PolicyVerification(
            UUID attemptId,
            String policyNumber,
            String productCode,
            String policyStatus,
            LocalDate coverStartDate,
            LocalDate coverEndDate,
            String currency,
            LocalDate lossDate,
            boolean covered) {}
}
