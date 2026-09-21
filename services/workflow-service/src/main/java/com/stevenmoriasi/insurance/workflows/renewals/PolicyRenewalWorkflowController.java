package com.stevenmoriasi.insurance.workflows.renewals;

import com.stevenmoriasi.insurance.workflows.renewals.PolicyRenewalWorkflowService.WorkflowReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/workflows/renewals")
@Validated
public class PolicyRenewalWorkflowController {

    private final PolicyRenewalWorkflowService workflows;

    public PolicyRenewalWorkflowController(PolicyRenewalWorkflowService workflows) {
        this.workflows = workflows;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public WorkflowReference start(@Valid @RequestBody RenewalInput input) {
        return workflows.start(input);
    }

    @GetMapping("/{policyNumber}")
    @PreAuthorize(
            "hasAnyRole('PROCESS_ANALYST','PROCESS_OWNER','FINANCE_OPERATOR','PLATFORM_ADMIN')")
    public RenewalStatus status(@PathVariable String policyNumber) {
        return workflows.status(policyNumber);
    }

    @PostMapping("/{policyNumber}/signals/customer-decision")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public void customerDecision(
            @PathVariable String policyNumber, @Valid @RequestBody DecisionRequest request) {
        workflows.customerDecision(policyNumber, request.decision());
    }

    @PostMapping("/{policyNumber}/signals/maker-checker")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public void makerChecker(
            @PathVariable String policyNumber, @RequestBody ApprovalRequest request) {
        workflows.makerCheckerDecision(policyNumber, request.approved());
    }

    @PostMapping("/{policyNumber}/signals/payment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('FINANCE_OPERATOR','PLATFORM_ADMIN')")
    public void payment(
            @PathVariable String policyNumber, @Valid @RequestBody ReferenceRequest request) {
        workflows.paymentReconciled(policyNumber, request.reference());
    }

    @PostMapping("/{policyNumber}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public void cancel(
            @PathVariable String policyNumber, @Valid @RequestBody ReasonRequest request) {
        workflows.cancel(policyNumber, request.reason());
    }

    public record DecisionRequest(@NotBlank String decision) {}

    public record ApprovalRequest(boolean approved) {}

    public record ReferenceRequest(@NotBlank String reference) {}

    public record ReasonRequest(@NotBlank String reason) {}
}
