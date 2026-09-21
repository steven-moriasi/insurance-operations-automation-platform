package com.stevenmoriasi.insurance.integrations.payments;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class PaymentCallbackVerifier {

    private final byte[] secret;

    PaymentCallbackVerifier(@Value("${insurance.payments.callback-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    boolean valid(String payload, String suppliedSignature) {
        if (suppliedSignature == null || suppliedSignature.isBlank()) {
            return false;
        }
        byte[] expected = hmac(payload);
        byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(suppliedSignature);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return MessageDigest.isEqual(expected, supplied);
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
