package com.stevenmoriasi.insurance.cases.domain;

public final class CaseTypes {

    private CaseTypes() {}

    public enum ClaimStatus {
        OPEN,
        AWAITING_EVIDENCE,
        UNDER_ASSESSMENT,
        AWAITING_APPROVAL,
        APPROVED,
        SETTLEMENT_INSTRUCTED,
        SETTLED,
        DECLINED,
        CLOSED
    }

    public enum TaskStatus {
        OPEN,
        CLAIMED,
        COMPLETED,
        CANCELLED
    }

    public enum EvidenceStatus {
        RECEIVED,
        ACCEPTED,
        REJECTED
    }

    public enum AssessmentOutcome {
        RECOMMEND_APPROVAL,
        REFER_FRAUD_REVIEW,
        REQUEST_MORE_EVIDENCE,
        RECOMMEND_DECLINE
    }

    public enum DecisionStatus {
        PENDING_CHECKER,
        APPROVED,
        REJECTED
    }

    public enum SettlementStatus {
        PENDING,
        INSTRUCTED,
        RECONCILED,
        FAILED,
        CANCELLED
    }

    public enum PartyType {
        CUSTOMER,
        CLAIMANT,
        BROKER,
        AGENT,
        SERVICE_PROVIDER
    }
}
