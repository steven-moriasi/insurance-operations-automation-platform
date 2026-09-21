package com.stevenmoriasi.insurance.cases.domain;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class ApprovalAuthorityPolicy {

    private final BigDecimal claimsApproverLimit;
    private final BigDecimal seniorApproverLimit;

    ApprovalAuthorityPolicy(
            @Value("${insurance.approval-limits.claims-approver:100000}")
                    BigDecimal claimsApproverLimit,
            @Value("${insurance.approval-limits.senior-claims-approver:1000000}")
                    BigDecimal seniorApproverLimit) {
        this.claimsApproverLimit = claimsApproverLimit;
        this.seniorApproverLimit = seniorApproverLimit;
    }

    void requireAuthority(CaseActor actor, BigDecimal amount) {
        if (actor.hasAnyRole("PLATFORM_ADMIN")) {
            return;
        }
        if (actor.hasAnyRole("SENIOR_CLAIMS_APPROVER")
                && amount.compareTo(seniorApproverLimit) <= 0) {
            return;
        }
        if (actor.hasAnyRole("CLAIMS_APPROVER") && amount.compareTo(claimsApproverLimit) <= 0) {
            return;
        }
        throw new CaseOperationException(
                CaseOperationException.Reason.AUTHORITY_EXCEEDED,
                "Approval amount exceeds the actor's authority limit");
    }
}
