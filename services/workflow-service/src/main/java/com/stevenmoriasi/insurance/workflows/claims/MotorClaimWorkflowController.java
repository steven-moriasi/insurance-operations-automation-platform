package com.stevenmoriasi.insurance.workflows.claims;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/internal/api/v1/workflows/claims")
@Validated
public class MotorClaimWorkflowController {

    private final MotorClaimWorkflowService workflows;

    public MotorClaimWorkflowController(MotorClaimWorkflowService workflows) {
        this.workflows = workflows;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER','CLAIMS_SUPERVISOR','PLATFORM_ADMIN')")
    public MotorClaimWorkflowService.WorkflowReference start(
            @Valid @RequestBody StartWorkflowRequest request) {
        return workflows.start(request.toInput());
    }

    @GetMapping("/{claimReference}")
    @PreAuthorize(
            "hasAnyRole('CLAIMS_OFFICER','CLAIMS_ASSESSOR','CLAIMS_APPROVER',"
                    + "'SENIOR_CLAIMS_APPROVER','CLAIMS_SUPERVISOR','FINANCE_OPERATOR',"
                    + "'PROCESS_OWNER','PLATFORM_ADMIN')")
    public MotorClaimStatus status(
            @PathVariable @Pattern(regexp = ClaimReference.PATTERN) String claimReference) {
        return workflows.status(claimReference);
    }

    @PostMapping("/{claimReference}/signals/evidence")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER','CLAIMS_SUPERVISOR','PLATFORM_ADMIN')")
    public void evidenceReceived(
            @PathVariable @Pattern(regexp = ClaimReference.PATTERN) String claimReference,
            @Valid @RequestBody SignalRequest request) {
        workflows.evidenceReceived(claimReference, request.reference());
    }

    @PostMapping("/{claimReference}/signals/assessment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('CLAIMS_ASSESSOR','CLAIMS_SUPERVISOR','PLATFORM_ADMIN')")
    public void assessmentSubmitted(
            @PathVariable @Pattern(regexp = ClaimReference.PATTERN) String claimReference,
            @Valid @RequestBody SignalRequest request) {
        workflows.assessmentSubmitted(claimReference, request.reference());
    }

    @PostMapping("/{claimReference}/signals/decision")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize(
            "hasAnyRole('CLAIMS_APPROVER','SENIOR_CLAIMS_APPROVER','CLAIMS_SUPERVISOR',"
                    + "'PLATFORM_ADMIN')")
    public void decisionRecorded(
            @PathVariable @Pattern(regexp = ClaimReference.PATTERN) String claimReference,
            @Valid @RequestBody SignalRequest request) {
        workflows.decisionRecorded(claimReference, request.reference());
    }

    @PostMapping("/{claimReference}/signals/settlement")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('FINANCE_OPERATOR','CLAIMS_SUPERVISOR','PLATFORM_ADMIN')")
    public void settlementReconciled(
            @PathVariable @Pattern(regexp = ClaimReference.PATTERN) String claimReference,
            @Valid @RequestBody SignalRequest request) {
        workflows.settlementReconciled(claimReference, request.reference());
    }

    @PostMapping("/{claimReference}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('CLAIMS_SUPERVISOR','PLATFORM_ADMIN')")
    public void cancel(
            @PathVariable @Pattern(regexp = ClaimReference.PATTERN) String claimReference,
            @Valid @RequestBody CancelRequest request) {
        workflows.cancel(claimReference, request.reason());
    }

    public record StartWorkflowRequest(
            @NotBlank @Pattern(regexp = ClaimReference.PATTERN) String claimReference,
            @Positive long evidenceSlaSeconds,
            @Positive long assessmentSlaSeconds,
            @Positive long decisionSlaSeconds,
            @Positive long settlementSlaSeconds) {

        MotorClaimInput toInput() {
            return new MotorClaimInput(
                    claimReference,
                    evidenceSlaSeconds,
                    assessmentSlaSeconds,
                    decisionSlaSeconds,
                    settlementSlaSeconds);
        }
    }

    public record SignalRequest(@NotBlank @Size(max = 128) String reference) {}

    public record CancelRequest(@NotBlank @Size(max = 256) String reason) {}

    private static final class ClaimReference {
        private static final String PATTERN = "[A-Z0-9][A-Z0-9_-]{0,63}";

        private ClaimReference() {}
    }
}
