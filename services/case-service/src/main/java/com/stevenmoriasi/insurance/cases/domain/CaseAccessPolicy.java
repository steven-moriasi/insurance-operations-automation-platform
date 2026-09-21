package com.stevenmoriasi.insurance.cases.domain;

import org.springframework.stereotype.Component;

@Component
class CaseAccessPolicy {

    void requireCaseAccess(ClaimCase claim, CaseActor actor) {
        if (actor.username().equals(claim.getOwnerUsername())
                || actor.hasAnyRole(
                        "CLAIMS_SUPERVISOR",
                        "CLAIMS_APPROVER",
                        "SENIOR_CLAIMS_APPROVER",
                        "PROCESS_OWNER",
                        "FINANCE_OPERATOR",
                        "PLATFORM_ADMIN")) {
            return;
        }
        throw forbidden("Actor cannot access this claim");
    }

    void requireAnyRole(CaseActor actor, String... roles) {
        if (!actor.hasAnyRole(roles)) {
            throw forbidden("Actor does not have the required role");
        }
    }

    private static CaseOperationException forbidden(String message) {
        return new CaseOperationException(CaseOperationException.Reason.FORBIDDEN, message);
    }
}
