package com.stevenmoriasi.insurance.integrations.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stevenmoriasi.insurance.integrations.domain.OutboxEventRepository;
import com.stevenmoriasi.insurance.integrations.payments.PaymentIntegrationService.PaymentCallback;
import com.stevenmoriasi.insurance.integrations.payments.PaymentIntegrationService.PaymentView;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationServiceTest {

    private static final String CALLBACK_SECRET = "integration-test-callback-secret";

    @Autowired private PaymentIntegrationService payments;
    @Autowired private PaymentInstructionRepository paymentRepository;
    @Autowired private OutboxEventRepository outbox;

    @Test
    void confirmsOnlyAnAuthenticExactPaymentCallback() throws Exception {
        PaymentView instructed =
                payments.instruct(
                        "payment-CLM-5001", "CLM-5001", new BigDecimal("125000.00"), "kes");
        PaymentCallback callback =
                new PaymentCallback(
                        instructed.providerReference(),
                        new BigDecimal("125000.00"),
                        "KES",
                        "SUCCESS");

        PaymentView confirmed = payments.callback(callback, sign(callback));
        PaymentView duplicate = payments.callback(callback, sign(callback));

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(duplicate).isEqualTo(confirmed);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(outbox.findAll())
                .extracting(event -> event.getEventType())
                .containsExactly("insurance.payment.instructed", "insurance.payment.confirmed");
    }

    @Test
    void rejectsSuccessfulCallbacksWithTheWrongAmount() throws Exception {
        PaymentView instructed =
                payments.instruct(
                        "payment-CLM-5002", "CLM-5002", new BigDecimal("125000.00"), "KES");
        PaymentCallback callback =
                new PaymentCallback(
                        instructed.providerReference(),
                        new BigDecimal("120000.00"),
                        "KES",
                        "SUCCESS");

        PaymentView rejected = payments.callback(callback, sign(callback));

        assertThat(rejected.status()).isEqualTo("FAILED");
        assertThat(rejected.failureReason()).contains("amount or currency");
    }

    @Test
    void rejectsCallbacksWithAnInvalidSignature() {
        PaymentView instructed =
                payments.instruct(
                        "payment-CLM-5003", "CLM-5003", new BigDecimal("50000.00"), "KES");
        PaymentCallback callback =
                new PaymentCallback(
                        instructed.providerReference(),
                        new BigDecimal("50000.00"),
                        "KES",
                        "SUCCESS");

        assertThatThrownBy(() -> payments.callback(callback, "invalid"))
                .isInstanceOf(InvalidCallbackSignatureException.class);
        assertThat(payments.get(instructed.id()).status()).isEqualTo("INSTRUCTED");
    }

    private static String sign(PaymentCallback callback) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(CALLBACK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of()
                .formatHex(
                        mac.doFinal(callback.canonicalPayload().getBytes(StandardCharsets.UTF_8)));
    }
}
