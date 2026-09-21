package com.stevenmoriasi.insurance.workflows.renewals;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RenewalInput(
        @NotBlank String policyNumber,
        boolean materialChangeRequired,
        @Min(1) long customerDecisionSlaSeconds,
        @Min(1) long makerCheckerSlaSeconds,
        @Min(1) long paymentSlaSeconds) {}
