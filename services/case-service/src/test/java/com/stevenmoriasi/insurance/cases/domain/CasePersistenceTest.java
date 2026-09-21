package com.stevenmoriasi.insurance.cases.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.stevenmoriasi.insurance.cases.domain.CaseTypes.PartyType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CasePersistenceTest {

    @Autowired private PolicyContextRepository policies;
    @Autowired private PartyRepository parties;
    @Autowired private ClaimCaseRepository claims;
    @Autowired private AuditEventRepository auditEvents;

    @Test
    void persistsCaseRelationshipsAndOrderedAuditHistory() {
        UUID policyId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        Instant reportedAt = Instant.parse("2026-09-21T08:00:00Z");

        policies.save(
                new PolicyContext(
                        policyId,
                        "POL-MTR-1001",
                        "PRIVATE_MOTOR",
                        "ACTIVE",
                        LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2026-12-31"),
                        "KES"));
        parties.save(
                new Party(
                        partyId,
                        PartyType.CLAIMANT,
                        "PTY-1001",
                        "Amina Wanjiku",
                        "+254700000001",
                        "amina@example.test"));
        claims.save(
                new ClaimCase(
                        claimId,
                        "CLM-1001",
                        policyId,
                        partyId,
                        LocalDate.parse("2026-09-20"),
                        reportedAt,
                        "claims.agent"));
        auditEvents.save(
                new AuditEvent(
                        UUID.randomUUID(),
                        "CLAIM",
                        claimId,
                        "CLAIM_REPORTED",
                        "claims.agent",
                        reportedAt,
                        "{\"claimReference\":\"CLM-1001\"}"));

        assertThat(claims.findByClaimReference("CLM-1001"))
                .get()
                .extracting(ClaimCase::getPolicyId, ClaimCase::getClaimantPartyId)
                .containsExactly(policyId, partyId);
        assertThat(auditEvents.findByAggregateIdOrderByOccurredAtAsc(claimId))
                .extracting(AuditEvent::getEventType)
                .containsExactly("CLAIM_REPORTED");
    }
}
