package com.stevenmoriasi.insurance.integrations.api;

import com.stevenmoriasi.insurance.integrations.legacy.PolicyVerificationService;
import com.stevenmoriasi.insurance.integrations.legacy.PolicyVerificationService.PolicyVerification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/integrations/policies")
@Validated
public class PolicyIntegrationController {

    private final PolicyVerificationService policyVerification;

    public PolicyIntegrationController(PolicyVerificationService policyVerification) {
        this.policyVerification = policyVerification;
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('WORKFLOW_OPERATOR','CLAIMS_OFFICER','PLATFORM_ADMIN')")
    public PolicyVerification verify(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody VerifyPolicyRequest request) {
        return policyVerification.verify(
                idempotencyKey, request.policyNumber(), request.lossDate());
    }

    public record VerifyPolicyRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{2,63}") String policyNumber,
            @NotNull LocalDate lossDate) {}
}
