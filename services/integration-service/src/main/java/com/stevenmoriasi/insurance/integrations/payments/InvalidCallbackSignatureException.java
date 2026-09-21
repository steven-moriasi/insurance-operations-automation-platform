package com.stevenmoriasi.insurance.integrations.payments;

public class InvalidCallbackSignatureException extends RuntimeException {

    public InvalidCallbackSignatureException() {
        super("Payment callback signature is invalid");
    }
}
