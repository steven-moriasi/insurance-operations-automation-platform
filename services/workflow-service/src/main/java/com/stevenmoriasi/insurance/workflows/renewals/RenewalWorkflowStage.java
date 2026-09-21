package com.stevenmoriasi.insurance.workflows.renewals;

public enum RenewalWorkflowStage {
    AWAITING_CUSTOMER_DECISION,
    AWAITING_MAKER_CHECKER,
    AWAITING_PAYMENT,
    COMPLETED,
    DECLINED,
    CANCELLED
}
