package com.stevenmoriasi.insurance.workflows.brokers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BrokerOnboardingInput(
        @NotBlank String brokerReference,
        @Min(1) long documentSlaSeconds,
        @Min(1) long automatedCheckSlaSeconds,
        @Min(1) long manualReviewSlaSeconds,
        @Min(1) long approvalSlaSeconds) {}
