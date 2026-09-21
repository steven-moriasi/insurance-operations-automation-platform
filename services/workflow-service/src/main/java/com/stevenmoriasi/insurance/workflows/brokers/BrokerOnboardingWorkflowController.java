package com.stevenmoriasi.insurance.workflows.brokers;

import com.stevenmoriasi.insurance.workflows.brokers.BrokerOnboardingWorkflowService.WorkflowReference;
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
@RequestMapping("/internal/api/v1/workflows/broker-onboarding")
@Validated
public class BrokerOnboardingWorkflowController {

    private final BrokerOnboardingWorkflowService workflows;

    public BrokerOnboardingWorkflowController(BrokerOnboardingWorkflowService workflows) {
        this.workflows = workflows;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public WorkflowReference start(@Valid @RequestBody BrokerOnboardingInput input) {
        return workflows.start(input);
    }

    @GetMapping("/{brokerReference}")
    @PreAuthorize("hasAnyRole('PROCESS_ANALYST','PROCESS_OWNER','PLATFORM_ADMIN')")
    public BrokerOnboardingStatus status(@PathVariable String brokerReference) {
        return workflows.status(brokerReference);
    }

    @PostMapping("/{brokerReference}/signals/documents")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public void documents(
            @PathVariable String brokerReference, @Valid @RequestBody ReferenceRequest request) {
        workflows.documentsReceived(brokerReference, request.reference());
    }

    @PostMapping("/{brokerReference}/signals/automated-check")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('AUTOMATION_WORKER','PROCESS_OWNER','PLATFORM_ADMIN')")
    public void automatedCheck(
            @PathVariable String brokerReference, @Valid @RequestBody OutcomeRequest request) {
        workflows.automatedCheckCompleted(brokerReference, request.outcome());
    }

    @PostMapping("/{brokerReference}/signals/manual-review")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public void manualReview(
            @PathVariable String brokerReference, @RequestBody ApprovalRequest request) {
        workflows.manualReviewCompleted(brokerReference, request.approved());
    }

    @PostMapping("/{brokerReference}/signals/approval")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public void approval(
            @PathVariable String brokerReference, @RequestBody ApprovalRequest request) {
        workflows.approvalRecorded(brokerReference, request.approved());
    }

    @PostMapping("/{brokerReference}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROCESS_OWNER','PLATFORM_ADMIN')")
    public void cancel(
            @PathVariable String brokerReference, @Valid @RequestBody ReasonRequest request) {
        workflows.cancel(brokerReference, request.reason());
    }

    public record ReferenceRequest(@NotBlank String reference) {}

    public record OutcomeRequest(@NotBlank String outcome) {}

    public record ApprovalRequest(boolean approved) {}

    public record ReasonRequest(@NotBlank String reason) {}
}
