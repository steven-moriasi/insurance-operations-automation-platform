package com.stevenmoriasi.insurance.integrations.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEvent;
import com.stevenmoriasi.insurance.integrations.domain.OutboxEventRepository;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationConflictException;
import com.stevenmoriasi.insurance.integrations.legacy.IntegrationNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentIntegrationService {

    private final PaymentInstructionRepository payments;
    private final OutboxEventRepository outbox;
    private final PaymentCallbackVerifier callbackVerifier;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public PaymentIntegrationService(
            PaymentInstructionRepository payments,
            OutboxEventRepository outbox,
            PaymentCallbackVerifier callbackVerifier,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.payments = payments;
        this.outbox = outbox;
        this.callbackVerifier = callbackVerifier;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public PaymentView instruct(
            String idempotencyKey, String claimReference, BigDecimal amount, String currency) {
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        PaymentInstruction existing = payments.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getClaimReference().equals(claimReference)
                    || existing.getAmount().compareTo(amount) != 0
                    || !existing.getCurrency().equals(normalizedCurrency)) {
                throw new IntegrationConflictException(
                        "Idempotency key was already used for a different payment");
            }
            return PaymentView.from(existing);
        }

        Instant now = clock.instant();
        PaymentInstruction payment =
                payments.save(
                        new PaymentInstruction(
                                UUID.randomUUID(),
                                idempotencyKey,
                                claimReference,
                                amount,
                                normalizedCurrency,
                                "SYN-PAY-" + UUID.randomUUID(),
                                now));
        writeEvent(payment, "insurance.payment.instructed", now);
        count("instructed");
        return PaymentView.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentView get(UUID paymentId) {
        return PaymentView.from(
                payments.findById(paymentId)
                        .orElseThrow(
                                () ->
                                        new IntegrationNotFoundException(
                                                "Payment instruction was not found")));
    }

    @Transactional
    public PaymentView callback(PaymentCallback callback, String signature) {
        String canonicalPayload = callback.canonicalPayload();
        if (!callbackVerifier.valid(canonicalPayload, signature)) {
            throw new InvalidCallbackSignatureException();
        }
        String callbackHash = sha256(canonicalPayload);
        PaymentInstruction payment =
                payments.findByProviderReference(callback.providerReference())
                        .orElseThrow(
                                () ->
                                        new IntegrationNotFoundException(
                                                "Payment provider reference was not found"));
        if (payment.getCallbackHash() != null) {
            if (payment.getCallbackHash().equals(callbackHash)) {
                count("duplicate_callback");
                return PaymentView.from(payment);
            }
            throw new IntegrationConflictException(
                    "A different callback was already recorded for this payment");
        }

        Instant now = clock.instant();
        boolean exactAmount = payment.getAmount().compareTo(callback.amount()) == 0;
        boolean exactCurrency = payment.getCurrency().equals(callback.currency());
        if ("SUCCESS".equals(callback.status()) && exactAmount && exactCurrency) {
            payment.confirm(callbackHash, now);
            writeEvent(payment, "insurance.payment.confirmed", now);
            count("confirmed");
        } else {
            String reason =
                    "SUCCESS".equals(callback.status())
                            ? "Callback amount or currency did not match the instruction"
                            : "Provider reported payment failure";
            payment.fail(callbackHash, reason, now);
            writeEvent(payment, "insurance.payment.rejected", now);
            count("rejected");
        }
        return PaymentView.from(payment);
    }

    private void writeEvent(PaymentInstruction payment, String eventType, Instant occurredAt) {
        PaymentView view = PaymentView.from(payment);
        try {
            outbox.save(
                    new OutboxEvent(
                            UUID.randomUUID(),
                            "PAYMENT",
                            payment.getId().toString(),
                            eventType,
                            objectMapper.writeValueAsString(view),
                            occurredAt));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Payment event cannot be serialized", exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void count(String outcome) {
        meterRegistry.counter("insurance.payment.operations", "outcome", outcome).increment();
    }

    public record PaymentCallback(
            String providerReference, BigDecimal amount, String currency, String status) {

        public PaymentCallback {
            currency = currency.toUpperCase(Locale.ROOT);
            status = status.toUpperCase(Locale.ROOT);
        }

        String canonicalPayload() {
            return providerReference + "|" + amount.toPlainString() + "|" + currency + "|" + status;
        }
    }

    public record PaymentView(
            UUID id,
            String claimReference,
            BigDecimal amount,
            String currency,
            String status,
            String providerReference,
            String failureReason,
            Instant createdAt,
            Instant confirmedAt) {

        static PaymentView from(PaymentInstruction payment) {
            return new PaymentView(
                    payment.getId(),
                    payment.getClaimReference(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getStatus().name(),
                    payment.getProviderReference(),
                    payment.getFailureReason(),
                    payment.getCreatedAt(),
                    payment.getConfirmedAt());
        }
    }
}
