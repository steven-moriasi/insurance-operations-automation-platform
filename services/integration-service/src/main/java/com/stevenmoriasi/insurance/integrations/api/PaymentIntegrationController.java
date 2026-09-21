package com.stevenmoriasi.insurance.integrations.api;

import com.stevenmoriasi.insurance.integrations.payments.PaymentIntegrationService;
import com.stevenmoriasi.insurance.integrations.payments.PaymentIntegrationService.PaymentCallback;
import com.stevenmoriasi.insurance.integrations.payments.PaymentIntegrationService.PaymentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class PaymentIntegrationController {

    private final PaymentIntegrationService payments;

    public PaymentIntegrationController(PaymentIntegrationService payments) {
        this.payments = payments;
    }

    @PostMapping("/internal/api/v1/integrations/payments")
    @PreAuthorize("hasAnyRole('WORKFLOW_OPERATOR','FINANCE_OPERATOR','PLATFORM_ADMIN')")
    public PaymentView instruct(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody InstructPaymentRequest request) {
        return payments.instruct(
                idempotencyKey, request.claimReference(), request.amount(), request.currency());
    }

    @GetMapping("/internal/api/v1/integrations/payments/{paymentId}")
    @PreAuthorize(
            "hasAnyRole('WORKFLOW_OPERATOR','FINANCE_OPERATOR','CLAIMS_SUPERVISOR',"
                    + "'PLATFORM_ADMIN')")
    public PaymentView get(@PathVariable UUID paymentId) {
        return payments.get(paymentId);
    }

    @PostMapping("/callbacks/payments/synthetic")
    public PaymentView callback(
            @RequestHeader("X-Payment-Signature") String signature,
            @Valid @RequestBody PaymentCallbackRequest request) {
        return payments.callback(request.toCommand(), signature);
    }

    public record InstructPaymentRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{0,63}") String claimReference,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {}

    public record PaymentCallbackRequest(
            @NotBlank @Size(max = 128) String providerReference,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotBlank @Pattern(regexp = "(?i)SUCCESS|FAILED") String status) {

        PaymentCallback toCommand() {
            return new PaymentCallback(providerReference, amount, currency, status);
        }
    }
}
