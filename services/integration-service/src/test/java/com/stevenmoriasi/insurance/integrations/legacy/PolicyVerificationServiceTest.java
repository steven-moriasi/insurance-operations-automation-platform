package com.stevenmoriasi.insurance.integrations.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.stevenmoriasi.insurance.integrations.domain.IntegrationAttemptRepository;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEventRepository;
import com.stevenmoriasi.insurance.integrations.legacy.LegacyPolicyGateway.LegacyPolicy;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PolicyVerificationServiceTest {

    @Autowired private PolicyVerificationService policyVerification;
    @Autowired private IntegrationAttemptRepository attempts;
    @Autowired private OutboxEventRepository outbox;

    @MockitoBean private LegacyPolicyGateway legacyPolicies;

    @Test
    void recordsAnIdempotentCoverageDecisionAndOutboxEvent() {
        when(legacyPolicies.findByPolicyNumber("POL-1001"))
                .thenReturn(
                        Optional.of(
                                new LegacyPolicy(
                                        "POL-1001",
                                        "PRIVATE_MOTOR",
                                        "ACTIVE",
                                        LocalDate.parse("2026-01-01"),
                                        LocalDate.parse("2026-12-31"),
                                        "KES")));

        PolicyVerificationService.PolicyVerification first =
                policyVerification.verify(
                        "verify-CLM-1001", "POL-1001", LocalDate.parse("2026-09-20"));
        PolicyVerificationService.PolicyVerification duplicate =
                policyVerification.verify(
                        "verify-CLM-1001", "POL-1001", LocalDate.parse("2026-09-20"));

        assertThat(first.covered()).isTrue();
        assertThat(duplicate).isEqualTo(first);
        assertThat(attempts.count()).isEqualTo(1);
        assertThat(outbox.findAll())
                .extracting(event -> event.getEventType())
                .containsExactly("insurance.policy.coverage-verified");
    }

    @Test
    void rejectsReuseOfAnIdempotencyKeyForDifferentInput() {
        when(legacyPolicies.findByPolicyNumber("POL-1002"))
                .thenReturn(
                        Optional.of(
                                new LegacyPolicy(
                                        "POL-1002",
                                        "PRIVATE_MOTOR",
                                        "ACTIVE",
                                        LocalDate.parse("2026-01-01"),
                                        LocalDate.parse("2026-12-31"),
                                        "KES")));
        policyVerification.verify("verify-CLM-1002", "POL-1002", LocalDate.parse("2026-09-20"));

        assertThatThrownBy(
                        () ->
                                policyVerification.verify(
                                        "verify-CLM-1002",
                                        "POL-1002",
                                        LocalDate.parse("2026-09-21")))
                .isInstanceOf(IntegrationConflictException.class);
    }
}
